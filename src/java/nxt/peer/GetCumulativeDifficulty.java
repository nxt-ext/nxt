package nxt.peer;

import nxt.Blockchain;
import org.json.simple.JSONObject;

final class GetCumulativeDifficulty extends HttpJSONRequestHandler {

    static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();

    private GetCumulativeDifficulty() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        response.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());

        return response;
    }

}
