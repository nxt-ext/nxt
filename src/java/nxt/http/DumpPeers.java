package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.stream.Collectors;

public final class DumpPeers extends APIServlet.APIRequestHandler {

    static final DumpPeers instance = new DumpPeers();

    private DumpPeers() {
        super(new APITag[] {APITag.DEBUG}, "version");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String version = Convert.nullToEmpty(req.getParameter("version"));
        Set<String> addresses = Peers.getAllPeers().parallelStream().unordered()
                .filter(peer -> peer.getState() == Peer.State.CONNECTED && peer.shareAddress() && !peer.isBlacklisted()
                        && peer.getVersion() != null && peer.getVersion().startsWith(version))
                .map(Peer::getAnnouncedAddress)
                .collect(Collectors.toSet());
        StringBuilder buf = new StringBuilder();
        StringBuilder buf2 = new StringBuilder();
        for (String address : addresses) {
            buf.append("('").append(address).append("'), ");
            buf2.append(address).append(";");
        }
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(buf.toString());
        jsonArray.add(buf2.toString());
        response.put("peers", jsonArray);
        return response;
    }

}
