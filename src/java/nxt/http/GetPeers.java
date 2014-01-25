package nxt.http;

import nxt.peer.Peer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetPeers extends HttpRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        JSONArray peers = new JSONArray();
        peers.addAll(Peer.peers.keySet());
        response.put("peers", peers);

        return response;
    }

}
