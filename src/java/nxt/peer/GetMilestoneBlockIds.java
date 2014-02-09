package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetMilestoneBlockIds extends HttpJSONRequestHandler {

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    JSONObject processJSONRequest(JSONObject request, Peer peer) {

        //TODO: add support for getting only a few milestoneBlockIds at a time
        JSONArray milestoneBlockIds = new JSONArray();
        Block block = Blockchain.getLastBlock();
        long blockId = block.getId();
        int height = block.getHeight();
        final int jumpLength = height * 4 / 1461 + 1;

        JSONObject response = new JSONObject();
        try {

            while (height > 0) {
                milestoneBlockIds.add(Convert.convert(blockId));
                blockId = Blockchain.getBlockIdAtHeight(height);
                height = height - jumpLength;
            }
            response.put("milestoneBlockIds", milestoneBlockIds);

        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString());
            response.put("error", e.toString());
        }

        return response;
    }

}
