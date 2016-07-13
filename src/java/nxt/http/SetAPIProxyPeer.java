package nxt.http;

import nxt.NxtException;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.PEER_NOT_CONNECTED;
import static nxt.http.JSONResponses.PEER_NOT_OPEN_API;
import static nxt.http.JSONResponses.UNKNOWN_PEER;

public class SetAPIProxyPeer extends APIServlet.APIRequestHandler {

    static final SetAPIProxyPeer instance = new SetAPIProxyPeer();

    private SetAPIProxyPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String peerAddress = Convert.emptyToNull(request.getParameter("peer"));
        if (peerAddress == null) {
            APIProxy.getInstance().setForcedPeer(null);
            return JSON.emptyJSON;
        }
        Peer peer = Peers.findOrCreatePeer(peerAddress, false);
        if (peer == null) {
            return UNKNOWN_PEER;
        }
        if (peer.getState() != Peer.State.CONNECTED ) {
            return PEER_NOT_CONNECTED;
        }
        if (!peer.isOpenAPI()) {
            return PEER_NOT_OPEN_API;
        }
        APIProxy.getInstance().setForcedPeer(peer);
        return JSONData.peer(peer);
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }


}
