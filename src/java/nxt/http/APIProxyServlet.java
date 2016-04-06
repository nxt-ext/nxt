package nxt.http;

import nxt.Nxt;
import nxt.peer.Peer;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class APIProxyServlet extends ProxyServlet {
    private static final boolean enableAPIProxy = Nxt.getBooleanProperty("nxt.enableAPIProxy");

    private final static Set<String> NOT_FORWARDED_REQUESTS;
    static {
        Set<String> notForwarded = new HashSet<>();
        notForwarded.add("broadcastTransaction");

        NOT_FORWARDED_REQUESTS = Collections.unmodifiableSet(notForwarded);
    }

    static void initClass() {}

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (true || enableAPIProxy && isForwardable(request)
                && Nxt.getBlockchainProcessor().isDownloading()) {
            super.service(request, response);
        } else {
            APIServlet apiServlet = new APIServlet();
            apiServlet.service(request, response);
        }
    }

    @Override
    protected void addProxyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {

    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        Peer servingPeer = APIProxy.getInstance().getServingPeer();


        StringBuilder uri = new StringBuilder();
        if (true) {
            uri.append("http://198.46.193.111:6876/nxt");
        } else {

            boolean useHttps = false; //servingPeer.providesService(Peer.Service.API_SSL);
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

    private boolean isForwardable(HttpServletRequest req) {
        String requestType = req.getParameter("requestType");
        if (NOT_FORWARDED_REQUESTS.contains(requestType)) {
            return false;
        }
        return true;
    }

    @Override
    protected Response.Listener newProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
        return new APIProxyResponseListener(request, response);
    }

    protected class APIProxyResponseListener extends ProxyServlet.ProxyResponseListener {

        protected APIProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        @Override
        public void onFailure(Response response, Throwable failure) {
            super.onFailure(response, failure);
            APIProxy.getInstance().blacklistHost(response.getRequest().getHost());
        }
    }
}
