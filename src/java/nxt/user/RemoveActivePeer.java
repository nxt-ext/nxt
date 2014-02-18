package nxt.user;

import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

import static nxt.user.JSONResponses.LOCAL_USERS_ONLY;

final class RemoveActivePeer extends UserServlet.UserRequestHandler {

    static final RemoveActivePeer instance = new RemoveActivePeer();

    private RemoveActivePeer() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        if (Users.allowedUserHosts == null && ! InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
            return LOCAL_USERS_ONLY;
        } else {
            int index = Integer.parseInt(req.getParameter("peer"));
            for (Peer peer : Peers.getAllPeers()) {
                if (Users.getIndex(peer) == index) {
                    if (! peer.isBlacklisted() && peer.getState() != Peer.State.NON_CONNECTED) {
                        peer.deactivate();
                    }
                    break;
                }
            }
        }
        return null;
    }
}
