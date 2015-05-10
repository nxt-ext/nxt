package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;
import java.util.stream.Collectors;

final class GetNextBlocks extends PeerServlet.PeerRequestHandler {

    static final GetNextBlocks instance = new GetNextBlocks();

    private GetNextBlocks() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        JSONArray nextBlocksArray = new JSONArray();
        List<? extends Block> blocks;
        long blockId = Convert.parseUnsignedLong((String)request.get("blockId"));
        List<String> stringList = (List<String>)request.get("blockIds");
        if (stringList != null) {
            List<Long> idList = stringList.stream().map(Convert::parseUnsignedLong).collect(Collectors.toList());
            blocks = Nxt.getBlockchain().getBlocksAfter(blockId, idList);
        } else {
            long limit = Convert.parseLong(request.get("limit"));
            blocks = Nxt.getBlockchain().getBlocksAfter(blockId, limit != 0 ? (int)limit : 720);
        }
        blocks.stream().forEach(block -> nextBlocksArray.add(block.getJSONObject()));
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

}
