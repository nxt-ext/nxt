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

import nxt.Constants;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.AsyncMiddleManServlet;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;

public final class APIProxyServlet extends AsyncMiddleManServlet {
    private static final Set<String> NOT_FORWARDED_REQUESTS;
    private static final Set<APITag> NOT_FORWARDED_TAGS;
    private static final String PROXY_REQUEST_ERROR = APIProxyServlet.class.getName() + ".proxyRequestError";

    static {
        Set<String> requests = new HashSet<>();
        requests.add("getConstants");
        requests.add("getPlugins");
        requests.add("getTime");
        requests.add("getBlockchainStatus");
        requests.add("getState");
        NOT_FORWARDED_REQUESTS = Collections.unmodifiableSet(requests);

        Set<APITag> tags = new HashSet<>();
        tags.add(APITag.UTILS);
        tags.add(APITag.DEBUG);
        tags.add(APITag.NETWORK);
        NOT_FORWARDED_TAGS = Collections.unmodifiableSet(tags);
    }

    private APIServlet apiServlet;

    static void initClass() {}

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
        if (Constants.isOffline) {
            //test only
            //uri.append("http://198.46.193.111:6876/nxt");
            uri.append("https://174.140.168.136:6877/nxt");
            //uri.append("http://nrs.scripterron.org:6876/nxt");
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

        APIServlet.APIRequestHandler apiRequestHandler = apiServlet.apiRequestHandlers.get(requestType);
        if (apiRequestHandler == null) {
            if (apiServlet.disabledRequestHandlers.containsKey(requestType)) {
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

        //Set intersection with pure Java
        HashSet<APITag> requestTags = new HashSet<>(apiRequestHandler.getAPITags());
        requestTags.retainAll(NOT_FORWARDED_TAGS);
        if (!requestTags.isEmpty()) {
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
        if (contentType != null && contentType.contains("multipart")) {
            return super.newClientRequestContentTransformer(clientRequest, proxyRequest);
        } else {
            return new PasswordFilteringContentTransformer(clientRequest);
        }

    }

    @Override
    protected ContentTransformer newServerResponseContentTransformer(HttpServletRequest clientRequest,
                                                                     HttpServletResponse proxyResponse,
                                                                     Response serverResponse) {
        return new ErrorMessageContentTransformer(clientRequest);
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
        private final HttpServletRequest clientRequest;
        private final PasswordFinder secretPhraseFinder = new PasswordFinder("secretPhrase=");
        private final PasswordFinder adminPasswordFinder = new PasswordFinder("adminPassword=");
        private boolean isPasswordDetected = false;

        public PasswordFilteringContentTransformer(HttpServletRequest clientRequest) {
            this.clientRequest = clientRequest;
        }

        @Override
        public void transform(ByteBuffer input, boolean finished, List<ByteBuffer> output) throws IOException {
            if (!isPasswordDetected) {
                int position = input.position();
                while (input.hasRemaining()) {
                    boolean secretPhaseFound = secretPhraseFinder.process(input);
                    boolean adminPasswordFound = false;
                    if (!secretPhaseFound) {
                        input.position(position);
                        adminPasswordFound = adminPasswordFinder.process(input);
                    }
                    if (secretPhaseFound || adminPasswordFound) {
                        isPasswordDetected = true;
                        clientRequest.setAttribute(PROXY_REQUEST_ERROR,
                                secretPhaseFound ? JSONResponses.PROXY_SECRET_PHRASE_DETECTED :
                                        JSONResponses.PROXY_ADMIN_PASSWORD_DETECTED);
                    }
                }
                input.position(position);

                if (!isPasswordDetected) {
                    output.add(input);
                }
            }
        }
    }

    private static class ErrorMessageContentTransformer implements AsyncMiddleManServlet.ContentTransformer {

        private final HttpServletRequest clientRequest;

        private ErrorMessageContentTransformer(HttpServletRequest clientRequest) {
            this.clientRequest = clientRequest;
        }

        @Override
        public void transform(ByteBuffer input, boolean finished, List<ByteBuffer> output) throws IOException {
            Object attributeObj = clientRequest.getAttribute(PROXY_REQUEST_ERROR);
            if (attributeObj != null) {
                if (finished) {
                    StringWriter sw = new StringWriter();
                    JSON.writeJSONString((JSONStreamAware) attributeObj, sw);
                    output.add(ByteBuffer.wrap(sw.getBuffer().toString().getBytes()));
                }
            } else {
                output.add(input);
            }
        }
    }
}
