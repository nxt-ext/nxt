package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetMilestoneBlockIds extends HttpJSONRequestHandler {

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray milestoneBlockIds = new JSONArray();
        Block block = Nxt.lastBlock.get();
        int jumpLength = block.height * 4 / 1461 + 1;
        while (block.height > 0) {

            milestoneBlockIds.add(block.getStringId());
            for (int i = 0; i < jumpLength && block.height > 0; i++) {

                block = Nxt.blocks.get(block.previousBlock);

            }

        }
        response.put("milestoneBlockIds", milestoneBlockIds);

        return response;
    }

}
