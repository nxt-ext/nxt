package nxt.peer;

import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

final class GetNextBlockIds extends HttpJSONRequestHandler {

    static final GetNextBlockIds instance = new GetNextBlockIds();

    private GetNextBlockIds() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray nextBlockIds = new JSONArray();
        Long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<Long> ids = Blockchain.getBlockIdsAfter(blockId, 1440);

        for (Long id : ids) {
            nextBlockIds.add(Convert.convert(id));
        }

        response.put("nextBlockIds", nextBlockIds);

        return response;
    }

}
