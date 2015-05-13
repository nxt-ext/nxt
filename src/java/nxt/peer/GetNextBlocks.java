package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.ArrayList;
import java.util.List;

final class GetNextBlocks extends PeerServlet.PeerRequestHandler {

    static final GetNextBlocks instance = new GetNextBlocks();

    private GetNextBlocks() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        JSONArray nextBlocksArray = new JSONArray();
        List<? extends Block> blocks;
        long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<String> stringList = (List<String>)request.get("blockIds");
        if (stringList != null) {
            List<Long> idList = new ArrayList<>();
            stringList.forEach(stringId -> idList.add(Convert.parseUnsignedLong(stringId)));
            blocks = Nxt.getBlockchain().getBlocksAfter(blockId, idList);
        } else {
            long limit = Convert.parseLong(request.get("limit"));
            blocks = Nxt.getBlockchain().getBlocksAfter(blockId, limit != 0 ? (int)limit : 720);
        }
        blocks.forEach(block -> nextBlocksArray.add(block.getJSONObject()));
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

}
