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

        long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<? extends Block> blocks = Nxt.getBlockchain().getBlocksAfter(blockId, 720);

        response.put("nextBlocks", blocks.stream().map(Block::getJSONObject).collect(Collectors.toCollection(JSONArray::new)));

        return response;
    }

}
