package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetMilestoneBlockIds extends HttpJSONRequestHandler {

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        //TODO: add support for getting only a few milestoneBlockIds at a time
        JSONArray milestoneBlockIds = new JSONArray();
        Block block = Blockchain.getLastBlock();
        long blockId = block.getId();
        int height = block.getHeight();
        final int jumpLength = height * 4 / 1461 + 1;

        while (height > 0) {
            milestoneBlockIds.add(Convert.convert(blockId));
            blockId = Blockchain.getBlockIdAtHeight(height);
            height = height - jumpLength;
        }

        response.put("milestoneBlockIds", milestoneBlockIds);

        return response;
    }

}
