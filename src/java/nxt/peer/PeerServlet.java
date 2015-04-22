package nxt.peer;

import nxt.util.CountingInputReader;
import nxt.util.CountingOutputWriter;
import nxt.util.JSON;
import nxt.util.Logger;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.servlets.gzip.CompressedResponseWrapper;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PeerServlet extends WebSocketServlet {

    abstract static class PeerRequestHandler {
        abstract JSONStreamAware processRequest(JSONObject request, Peer peer);
    }

    private static final Map<String,PeerRequestHandler> peerRequestHandlers;

    static {
        Map<String,PeerRequestHandler> map = new HashMap<>();
        map.put("addPeers", AddPeers.instance);
        map.put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
        map.put("getInfo", GetInfo.instance);
        map.put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
        map.put("getNextBlockIds", GetNextBlockIds.instance);
        map.put("getNextBlocks", GetNextBlocks.instance);
        map.put("getPeers", GetPeers.instance);
        map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
        map.put("processBlock", ProcessBlock.instance);
        map.put("processTransactions", ProcessTransactions.instance);
        peerRequestHandlers = Collections.unmodifiableMap(map);
    }

    private static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_REQUEST_TYPE);
        UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
    }

    private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNSUPPORTED_PROTOCOL);
        UNSUPPORTED_PROTOCOL = JSON.prepare(response);
    }

    private static final JSONStreamAware BLACKLISTED;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.BLACKLISTED);
        BLACKLISTED = JSON.prepare(response);
    }

    private static final JSONStreamAware UNKNOWN_PEER;
    static {
        JSONObject response = new JSONObject();
        response.put("error", Errors.UNKNOWN_PEER);
        UNKNOWN_PEER = JSON.prepare(response);
    }

    private boolean isGzipEnabled;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        isGzipEnabled = Boolean.parseBoolean(config.getInitParameter("isGzipEnabled"));
    }

    /**
     * Configure the WebSocket factory
     *
     * @param   factory             WebSocket factory
     */
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(Peers.webSocketIdleTimeout);
        factory.getPolicy().setMaxBinaryMessageSize(PeerWebSocket.MAX_MESSAGE_SIZE);
        factory.setCreator(new PeerSocketCreator());
    }

    /**
     * Process HTTP POST request
     *
     * @param   req                 HTTP request
     * @param   resp                HTTP response
     * @throws  ServletException    Servlet processing error
     * @throws  IOException         I/O error
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //
        PeerImpl peer = Peers.findOrCreatePeer(req.getRemoteAddr(), -1, null, true);
        if (peer == null)
            jsonResponse = UNKNOWN_PEER;
        else
            jsonResponse = process(peer, req.getReader());
        //
        // Return the response
        //
        resp.setContentType("text/plain; charset=UTF-8");
        try {
            long byteCount;
            try (CountingOutputWriter writer = new CountingOutputWriter(resp.getWriter())) {
                jsonResponse.writeJSONString(writer);
                if (isGzipEnabled)
                    byteCount = ((Response)((CompressedResponseWrapper)resp).getResponse()).getContentCount();
                else
                    byteCount = writer.getCount();
            }
            if (peer != null)
                peer.updateUploadedVolume(byteCount);
        } catch (RuntimeException | IOException e) {
            if (peer != null)
                peer.blacklist(e);
            throw e;
        }
    }

    /**
     * Process WebSocket POST request
     *
     * @param   webSocket           WebSocket for the connection
     * @param   requestId           Request identifier
     * @param   request             Request message
     */
    void doPost(PeerWebSocket webSocket, long requestId, String request) {
        JSONStreamAware jsonResponse;
        //
        // Process the peer request
        //
        String remoteAddr = webSocket.getSession().getRemoteAddress().getHostString();
        PeerImpl peer = Peers.findOrCreatePeer(remoteAddr, -1, null, true);
        if (peer == null)
            jsonResponse = UNKNOWN_PEER;
        else
            jsonResponse = process(peer, new StringReader(request));
        //
        // Return the response
        //
        try {
            StringWriter writer = new StringWriter(1000);
            jsonResponse.writeJSONString(writer);
            String response = writer.toString();
            webSocket.sendResponse(requestId, response);
            if (peer != null)
                peer.updateUploadedVolume(response.length());
        } catch (RuntimeException | IOException e) {
            if (peer != null) {
                if (e instanceof RuntimeException)
                    Logger.logDebugMessage(String.format("Send failed to peer %s",
                        peer.getAnnouncedAddress()!=null ? peer.getAnnouncedAddress() : peer.getPeerAddress()), e);
                else
                    Logger.logDebugMessage(String.format("Send failed to peer %s: %s",
                        peer.getAnnouncedAddress()!=null ? peer.getAnnouncedAddress() : peer.getPeerAddress(),
                        e.getMessage()!=null ? e.getMessage() : e.toString()));
                if (!(e instanceof IOException))
                    peer.blacklist(e);
            }
        }
    }

    /**
     * Process the peer request
     *
     * @param   peer                Peer
     * @param   inputReader         Input reader
     * @return                      JSON response
     */
    private JSONStreamAware process(PeerImpl peer, Reader inputReader) {
        JSONStreamAware response;
        //
        // Check for blacklisted peer
        //
        if (peer.isBlacklisted()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", Errors.BLACKLISTED);
            jsonObject.put("cause", peer.getBlacklistingCause());
            return jsonObject;
        }
        Peers.addPeer(peer);
        //
        // Process the request
        //
        try {
            JSONObject request;
            try (CountingInputReader cr = new CountingInputReader(inputReader, Peers.MAX_REQUEST_SIZE)) {
                request = (JSONObject)JSONValue.parseWithException(cr);
                peer.updateDownloadedVolume(cr.getCount());
            }
            if (request == null) {
                response = UNSUPPORTED_REQUEST_TYPE;
            } else if (request.get("protocol") != null && ((Number)request.get("protocol")).intValue() == 1) {
                PeerRequestHandler peerRequestHandler = peerRequestHandlers.get((String)request.get("requestType"));
                if (peerRequestHandler != null) {
                    response = peerRequestHandler.processRequest(request, peer);
                } else {
                    response = UNSUPPORTED_REQUEST_TYPE;
                }
            } else {
                Logger.logDebugMessage("Unsupported protocol " + request.get("protocol"));
                response = UNSUPPORTED_PROTOCOL;
            }
        } catch (RuntimeException|ParseException|IOException e) {
            Logger.logDebugMessage("Error processing POST request: " + e.toString());
            peer.blacklist(e);
            JSONObject json = new JSONObject();
            json.put("error", e.toString());
            response = json;
        }
        return response;
    }

    /**
     * WebSocket creator for peer connections
     */
    private class PeerSocketCreator implements WebSocketCreator  {
        /**
         * Create a peer WebSocket
         *
         * @param   req             WebSocket upgrade request
         * @param   resp            WebSocket upgrade response
         * @return                  WebSocket
         */
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            return Peers.useWebSockets ? new PeerWebSocket(PeerServlet.this) : null;
        }
    }
}
