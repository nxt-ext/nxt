package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.stream.Collectors;

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
        JSONObject response = new JSONObject();
        response.put("peers", peers.parallelStream().unordered()
                .map(includePeerInfo ? JSONData::peer : Peer::getPeerAddress)
                .collect(Collectors.toCollection(JSONArray::new)));
        return response;
    }

}
