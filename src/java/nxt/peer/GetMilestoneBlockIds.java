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

            JSONArray milestoneBlockIds = new JSONArray();

            String lastBlockIdString = (String) request.get("lastBlockId");
            if (lastBlockIdString != null) {
                Long lastBlockId = Convert.parseUnsignedLong(lastBlockIdString);
                Long myLastBlockId = Blockchain.getLastBlock().getId();
                if (myLastBlockId.equals(lastBlockId) || Blockchain.hasBlock(lastBlockId)) {
                    milestoneBlockIds.add(lastBlockIdString);
                    response.put("milestoneBlockIds", milestoneBlockIds);
                    if (myLastBlockId.equals(lastBlockId)) {
                        response.put("last", Boolean.TRUE);
                    }
                    return response;
                }
            }

            long blockId;
            int height;
            int jump;
            int limit;
            String lastMilestoneBlockIdString = (String) request.get("lastMilestoneBlockId");
            if (lastMilestoneBlockIdString != null) {
                Block lastMilestoneBlock = Blockchain.getBlock(Convert.parseUnsignedLong(lastMilestoneBlockIdString));
                if (lastMilestoneBlock == null) {
                    throw new IllegalStateException("Don't have block " + lastMilestoneBlockIdString);
                }
                height = lastMilestoneBlock.getHeight();
                jump = Math.min(1440, Blockchain.getLastBlock().getHeight() - height);
                height = Math.max(height - jump, 0);
                limit = 10;
            } else if (lastBlockIdString != null) {
                height = Blockchain.getLastBlock().getHeight();
                jump = 10;
                limit = 10;
            } else {
                peer.blacklist();
                response.put("error", "Old getMilestoneBlockIds protocol not supported, please upgrade");
                return response;
            }
            blockId = Blockchain.getBlockIdAtHeight(height);

            while (height > 0 && limit-- > 0) {
                milestoneBlockIds.add(Convert.convert(blockId));
                blockId = Blockchain.getBlockIdAtHeight(height);
                height = height - jump;
            }
            response.put("milestoneBlockIds", milestoneBlockIds);

        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString());
            response.put("error", e.toString());
        }

        return response;
    }

}
