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

        JSONObject response = new JSONObject();
        try {

            String lastBlockId = (String) request.get("lastMilestoneBlockId");
            boolean batch = request.get("batch") != null;

            long blockId;
            int jumpLength;
            int height;
            int limit = 10;

            if (lastBlockId != null) {
                Block block = Blockchain.getBlock(Convert.parseUnsignedLong(lastBlockId));
                if (block == null) {
                    throw new IllegalStateException("Don't have block " + lastBlockId);
                }
                jumpLength = ((Number)request.get("jumpLength")).intValue();
                height = Math.max(block.getHeight() - jumpLength, 0);
                blockId = Blockchain.getBlockIdAtHeight(height);
            } else {
                Block block = Blockchain.getLastBlock();
                blockId = block.getId();
                height = block.getHeight();
                jumpLength = height * 4 / 1461 + 1;
                if (! batch) {
                    limit = height;
                }
            }

            JSONArray milestoneBlockIds = new JSONArray();
            while (height > 0 && limit-- > 0) {
                milestoneBlockIds.add(Convert.convert(blockId));
                blockId = Blockchain.getBlockIdAtHeight(height);
                height = height - jumpLength;
            }
            response.put("milestoneBlockIds", milestoneBlockIds);
            response.put("jumpLength", jumpLength);

        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString());
            response.put("error", e.toString());
        }

        return response;
    }

}
