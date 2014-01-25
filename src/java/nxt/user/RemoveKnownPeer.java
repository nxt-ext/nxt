package nxt.user;

import nxt.Nxt;
import nxt.peer.Peer;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

final class RemoveKnownPeer extends UserRequestHandler {

    static final RemoveKnownPeer instance = new RemoveKnownPeer();

    private RemoveKnownPeer() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req, User user) throws IOException {
        if (Nxt.allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
            JSONObject response = new JSONObject();
            response.put("response", "showMessage");
            response.put("message", "This operation is allowed to local host users only!");
            return response;
        } else {
            int index = Integer.parseInt(req.getParameter("peer"));
            for (Peer peer : Peer.peers.values()) {
                if (peer.index == index) {
                    peer.removePeer();
                    break;
                }
            }
        }
        return null;
    }
}
