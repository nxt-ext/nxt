package nxt.peer;

import nxt.Blockchain;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetCumulativeDifficulty extends HttpJSONRequestHandler {

    static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();

    private GetCumulativeDifficulty() {}


    @Override
    JSONStreamAware processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        response.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());

        return response;
    }

}
