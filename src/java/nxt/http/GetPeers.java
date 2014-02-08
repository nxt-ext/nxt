package nxt.http;

import nxt.peer.Peer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPeers extends HttpRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray peers = new JSONArray();
        for (Peer peer : Peer.getAllPeers()) {
            peers.add(peer.getPeerAddress());
        }

        JSONObject response = new JSONObject();
        response.put("peers", peers);
        return response;
    }

}
