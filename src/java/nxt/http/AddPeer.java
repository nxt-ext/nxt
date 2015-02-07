package nxt.http;

import nxt.NxtException;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_PEER;

public class AddPeer extends APIRequestHandler {

    static final AddPeer instance = new AddPeer();
    
    private AddPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request)
            throws NxtException {
        final String peerAddress = request.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        JSONObject response = new JSONObject();
        if (Peers.hasTooManyKnownPeers()) {
            response.put("errorCode", 7);
            response.put("errorDescription", "Too many known peers");
        } else {
            Peer peer = Peers.findOrCreatePeer(peerAddress, true);
            if (peer != null) {
                Peers.connectPeer(peer);
                response = JSONData.peer(peer);
                if (Peers.addPeer(peer)) {
                    response.put("isNewlyAdded", true);
                }
            } else {
                response.put("errorCode", 8);
                response.put("errorDescription", "Failed to add peer");
            }
        }
        return response;
    }

}
