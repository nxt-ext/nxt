package nxt.http;

import nxt.NxtException;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.*;

public class SetAPIProxyPeer extends APIServlet.APIRequestHandler {

    static final SetAPIProxyPeer instance = new SetAPIProxyPeer();

    private SetAPIProxyPeer() {
        super(new APITag[] {APITag.NETWORK}, "peerAddress");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String peerAddress = Convert.emptyToNull(request.getParameter("peerAddress"));
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        Peer peer = Peers.findOrCreatePeer(peerAddress, false);
        if (peer == null) {
            return UNKNOWN_PEER;
        }

        if (peer.getState() != Peer.State.CONNECTED ) {
            return PEER_NOT_CONNECTED;
        }
        if (!APIProxy.isOpenAPIPeer(peer)) {
            return PEER_NOT_OPEN_API;
        }

        APIProxy.getInstance().setServingPeer(peer);
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
