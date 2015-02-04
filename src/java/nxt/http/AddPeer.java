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
        JSONObject response = new JSONObject();
        
        String peerAddress = request.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        
        Peer peer = Peers.addPeer(peerAddress);
        
        if (peer != null) {
            Peers.connectPeer(peer);
            response = JSONData.peer(peer);
        } else {
            response.put("error", "Failed to add peer");
        }
        
        return response;
    }

}
