package nxt.user;

import nxt.Nxt;
import nxt.peer.Peer;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

import static nxt.user.JSONResponses.LOCAL_USERS_ONLY;

final class RemoveBlacklistedPeer extends UserRequestHandler {

    static final RemoveBlacklistedPeer instance = new RemoveBlacklistedPeer();

    private RemoveBlacklistedPeer() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        if (Nxt.allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {
            return LOCAL_USERS_ONLY;
        } else {
            int index = Integer.parseInt(req.getParameter("peer"));
            for (Peer peer : Peer.allPeers) {
                if (peer.index == index) {
                    if (peer.getBlacklistingTime() > 0) {
                        peer.removeBlacklistedStatus();
                    }
                    break;
                }
            }
        }
        return null;
    }
}
