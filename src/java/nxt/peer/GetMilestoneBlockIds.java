package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetMilestoneBlockIds extends HttpJSONRequestHandler {

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray milestoneBlockIds = new JSONArray();
        Block block = Blockchain.getLastBlock();
        int jumpLength = block.getHeight() * 4 / 1461 + 1;
        while (block != null && block.getHeight() > 0) {

            milestoneBlockIds.add(block.getStringId());
            for (int i = 0; i < jumpLength && block != null && block.getHeight() > 0; i++) {

                block = Blockchain.getBlock(block.getPreviousBlockId());

            }

        }
        response.put("milestoneBlockIds", milestoneBlockIds);

        return response;
    }

}
