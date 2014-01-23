package nxt.peer;

import nxt.Nxt;
import org.json.simple.JSONObject;

final class GetCumulativeDifficulty extends HttpJSONRequestHandler {

    static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();

    private GetCumulativeDifficulty() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        response.put("cumulativeDifficulty", Nxt.lastBlock.get().cumulativeDifficulty.toString());

        return response;
    }

}
