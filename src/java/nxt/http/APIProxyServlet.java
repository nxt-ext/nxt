/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.AsyncMiddleManServlet;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class APIProxyServlet extends AsyncMiddleManServlet {
    private static final Set<String> NOT_FORWARDED_REQUESTS;
    private static final Set<APITag> NOT_FORWARDED_TAGS;

    static {
        Set<String> requests = new HashSet<>();
        requests.add("getBlockchainStatus");
        requests.add("getState");
        requests.add("getForging");
        requests.add("startForging");
        requests.add("stopForging");
        requests.add("getFundingMonitor");
        requests.add("startFundingMonitor");
        requests.add("stopFundingMonitor");
        requests.add("getShufflers");
        requests.add("startShuffler");
        requests.add("stopShuffler");

        NOT_FORWARDED_REQUESTS = Collections.unmodifiableSet(requests);

        Set<APITag> tags = new HashSet<>();
        tags.add(APITag.UTILS);
        tags.add(APITag.DEBUG);
        tags.add(APITag.NETWORK);
        NOT_FORWARDED_TAGS = Collections.unmodifiableSet(tags);
    }

    private APIServlet apiServlet;

    static void initClass() {
        APIProxy.init();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (apiServlet == null) {
                apiServlet = new APIServlet();
            }
            if (APIProxy.isActivated() && isForwardable(request)) {
                super.service(request, response);
            } else {
                apiServlet.service(request, response);
            }
        } catch (ParameterException e) {
            try (Writer writer = response.getWriter()) {
                JSON.writeJSONString(e.getErrorResponse(), writer);
            }
        }
    }

    @Override
    protected void addProxyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {

    }

    @Override
    protected HttpClient newHttpClient() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        
        sslContextFactory.addExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        sslContextFactory.addExcludeProtocols("SSLv3");
        sslContextFactory.setTrustAll(true);

        return new HttpClient(sslContextFactory);
    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {

        StringBuilder uri = new StringBuilder();
        if (!APIProxy.forcedServerURL.isEmpty()) {
            uri.append(APIProxy.forcedServerURL);
        } else {
            Peer servingPeer = APIProxy.getInstance().getServingPeer();
            boolean useHttps = servingPeer.providesService(Peer.Service.API_SSL);
            if (useHttps) {
                uri.append("https://");
            } else {
                uri.append("http://");
            }
            uri.append(servingPeer.getHost()).append(":");
            if (useHttps) {
                uri.append(servingPeer.getApiSSLPort());
            } else {
                uri.append(servingPeer.getApiPort());
            }
            uri.append("/nxt");
        }

        String query = clientRequest.getQueryString();
        if (query != null) {
            uri.append("?").append(query);
        }
        URI rewrittenURI = URI.create(uri.toString()).normalize();
        return rewrittenURI.toString();
    }

    @Override
    protected void onClientRequestFailure(HttpServletRequest clientRequest, Request proxyRequest,
                                          HttpServletResponse proxyResponse, Throwable failure) {
        if (failure instanceof PasswordDetectedException) {
            PasswordDetectedException passwordDetectedException = (PasswordDetectedException) failure;
            try (Writer writer = proxyResponse.getWriter()) {
                JSON.writeJSONString(passwordDetectedException.errorResponse, writer);
                sendProxyResponseError(clientRequest, proxyResponse, HttpStatus.OK_200);
            } catch (IOException e) {
                e.addSuppressed(failure);
                super.onClientRequestFailure(clientRequest, proxyRequest, proxyResponse, e);
            }
        } else {
            super.onClientRequestFailure(clientRequest, proxyRequest, proxyResponse, failure);
        }
    }

    private boolean isForwardable(HttpServletRequest req) throws ParameterException {
        MultiMap<String> parameters = new MultiMap<>();
        String queryString = req.getQueryString();
        String requestType = null;

        if (queryString != null) {
            UrlEncoded.decodeUtf8To(queryString, parameters);
            requestType = parameters.getString("requestType");
        }

        if (Convert.emptyToNull(requestType) == null) {
            throw new ParameterException(JSONResponses.PROXY_MISSING_REQUEST_TYPE);
        }

        APIServlet.APIRequestHandler apiRequestHandler = APIServlet.apiRequestHandlers.get(requestType);
        if (apiRequestHandler == null) {
            if (APIServlet.disabledRequestHandlers.containsKey(requestType)) {
                throw new ParameterException(JSONResponses.ERROR_DISABLED);
            } else {
                throw new ParameterException(JSONResponses.ERROR_INCORRECT_REQUEST);
            }
        }

        if (!apiRequestHandler.requireBlockchain()) {
            return false;
        }

        if (NOT_FORWARDED_REQUESTS.contains(requestType)) {
            return false;
        }

        if (!Collections.disjoint(apiRequestHandler.getAPITags(), NOT_FORWARDED_TAGS)) {
            return false;
        }

        if (parameters.containsKey("secretPhrase")) {
            throw new ParameterException(JSONResponses.PROXY_SECRET_PHRASE_DETECTED);
        }

        if (parameters.containsKey("adminPassword")) {
            throw new ParameterException(JSONResponses.PROXY_ADMIN_PASSWORD_DETECTED);
        }

        return true;
    }

    @Override
    protected Response.Listener newProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
        return new APIProxyResponseListener(request, response);
    }

    protected class APIProxyResponseListener extends AsyncMiddleManServlet.ProxyResponseListener {

        protected APIProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        @Override
        public void onFailure(Response response, Throwable failure) {
            super.onFailure(response, failure);
            Logger.logErrorMessage("proxy failed", failure);
            APIProxy.getInstance().blacklistHost(response.getRequest().getHost());
        }
    }

    @Override
    protected ContentTransformer newClientRequestContentTransformer(HttpServletRequest clientRequest,
                                                                    Request proxyRequest) {

        String contentType = clientRequest.getContentType();
        Logger.logDebugMessage(System.identityHashCode(clientRequest) + " newClientRequestContentTransformer " + contentType);
        if (contentType != null && contentType.contains("multipart")) {
            return super.newClientRequestContentTransformer(clientRequest, proxyRequest);
        } else {
            return new PasswordFilteringContentTransformer(clientRequest);
        }

    }

    private static class PasswordDetectedException extends RuntimeException {
        private final JSONStreamAware errorResponse;

        private PasswordDetectedException(JSONStreamAware errorResponse) {
            this.errorResponse = errorResponse;
        }
    }

    private static class PasswordFinder {
        private final byte[] token;
        private int state;

        private PasswordFinder(String tokenStr) {
            token = tokenStr.getBytes();
        }

        private boolean process(ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                int current = buffer.get() & 0xFF;
                if (state < token.length) {
                    if (current != token[state]) {
                        state = 0;
                        continue;
                    }

                    ++state;
                    if (state == token.length)
                        return true;
                }
            }
            return state == token.length;
        }
    }

    private static class PasswordFilteringContentTransformer implements AsyncMiddleManServlet.ContentTransformer {
        private final int clientRequestId;
        private final PasswordFinder secretPhraseFinder = new PasswordFinder("secretPhrase=");
        private final PasswordFinder adminPasswordFinder = new PasswordFinder("adminPassword=");
        private boolean isPasswordDetected = false;

        public PasswordFilteringContentTransformer(HttpServletRequest clientRequest) {
            this.clientRequestId = System.identityHashCode(clientRequest);
        }

        @Override
        public void transform(ByteBuffer input, boolean finished, List<ByteBuffer> output) throws IOException {
            Logger.logDebugMessage(clientRequestId + " PasswordFilteringContentTransformer.transform " + isPasswordDetected + " " + input.position());
            if (!isPasswordDetected) {
                boolean positionChanged = false;
                int position = input.position();
                while (input.hasRemaining()) {
                    positionChanged = true;
                    boolean secretPhaseFound = secretPhraseFinder.process(input);
                    boolean adminPasswordFound = false;
                    if (!secretPhaseFound) {
                        input.position(position);
                        adminPasswordFound = adminPasswordFinder.process(input);
                    }
                    if (secretPhaseFound || adminPasswordFound) {
                        isPasswordDetected = true;
                        JSONStreamAware error = secretPhaseFound ? JSONResponses.PROXY_SECRET_PHRASE_DETECTED :
                                JSONResponses.PROXY_ADMIN_PASSWORD_DETECTED;

                        throw new PasswordDetectedException(error);
                    }
                }
                if (positionChanged) {
                    input.position(position);
                }

                if (!isPasswordDetected) {
                    output.add(input);
                }
            }
        }
    }
}
