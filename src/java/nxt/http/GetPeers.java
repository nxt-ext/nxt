package nxt.http;

import nxt.peer.Peer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class GetPeers extends HttpRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray peers = new JSONArray();
        for (Peer peer : Peer.allPeers) {
            peers.add(peer.peerAddress);
        }

        JSONObject response = new JSONObject();
        response.put("peers", peers);
        return response;
    }

}
