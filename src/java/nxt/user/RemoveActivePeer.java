package nxt.user;

import nxt.Nxt;
import nxt.peer.Peer;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

import static nxt.user.JSONResponses.LOCAL_USERS_ONLY;

final class RemoveActivePeer extends UserRequestHandler {

    static final RemoveActivePeer instance = new RemoveActivePeer();

    private RemoveActivePeer() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        if (Nxt.allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
            return LOCAL_USERS_ONLY;
        } else {
            int index = Integer.parseInt(req.getParameter("peer"));
            for (Peer peer : Peer.getAllPeers()) {
                if (peer.getIndex() == index) {
                    if (peer.getBlacklistingTime() == 0 && peer.getState() != Peer.State.NON_CONNECTED) {
                        peer.deactivate();
                    }
                    break;
                }
            }
        }
        return null;
    }
}
