package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;

final class GetNextBlocks extends PeerServlet.PeerRequestHandler {

    static final GetNextBlocks instance = new GetNextBlocks();

    private GetNextBlocks() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        JSONArray nextBlocksArray = new JSONArray();

        long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<? extends Block> blocks = Nxt.getBlockchain().getBlocksAfter(blockId, 720);

        for (Block block : blocks) {
            nextBlocksArray.add(block.getJSONObject());
        }
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

}
