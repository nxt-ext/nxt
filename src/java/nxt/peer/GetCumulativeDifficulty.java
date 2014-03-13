package nxt.peer;

import nxt.Nxt;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetCumulativeDifficulty extends PeerServlet.PeerRequestHandler {

    static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();

    private GetCumulativeDifficulty() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        response.put("cumulativeDifficulty", Nxt.getBlockchain().getLastBlock().getCumulativeDifficulty().toString());

        return response;
    }

}
