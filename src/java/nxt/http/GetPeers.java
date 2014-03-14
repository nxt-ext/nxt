package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

public final class GetPeers extends APIServlet.APIRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}

    @Override
    List<String> getParameters() {
        return Collections.emptyList();
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray peers = new JSONArray();
        for (Peer peer : Peers.getAllPeers()) {
            peers.add(peer.getPeerAddress());
        }

        JSONObject response = new JSONObject();
        response.put("peers", peers);
        return response;
    }

}
