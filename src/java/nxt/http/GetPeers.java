package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public final class GetPeers extends APIServlet.APIRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {
        super(new APITag[] {APITag.NETWORK}, "active", "state", "includePeerInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        boolean active = "true".equalsIgnoreCase(req.getParameter("active"));
        String stateValue = Convert.emptyToNull(req.getParameter("state"));
        boolean includePeerInfo = "true".equalsIgnoreCase(req.getParameter("includePeerInfo"));

        Collection<? extends Peer> peers = active ? Peers.getActivePeers() : stateValue != null ? Peers.getPeers(Peer.State.valueOf(stateValue)) : Peers.getAllPeers();
        JSONArray peersJSON = new JSONArray();
        for (Peer peer : peers) {
            peersJSON.add(includePeerInfo ? JSONData.peer(peer) : peer.getPeerAddress());
        }

        JSONObject response = new JSONObject();
        response.put("peers", peersJSON);
        return response;
    }

}
