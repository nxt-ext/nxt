package nxt.user;

import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

import static nxt.user.JSONResponses.LOCAL_USERS_ONLY;

final class RemoveKnownPeer extends UserServlet.UserRequestHandler {

    static final RemoveKnownPeer instance = new RemoveKnownPeer();

    private RemoveKnownPeer() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        if (Users.allowedUserHosts == null && ! InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
            return LOCAL_USERS_ONLY;
        } else {
            int index = Integer.parseInt(req.getParameter("peer"));
            for (Peer peer : Peers.getAllPeers()) {
                if (Users.getIndex(peer) == index) {
                    peer.remove();
                    break;
                }
            }
        }
        return null;
    }
}
