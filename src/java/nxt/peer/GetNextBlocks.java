package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class GetNextBlocks extends HttpJSONRequestHandler {

    static final GetNextBlocks instance = new GetNextBlocks();

    private GetNextBlocks() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        List<Block> nextBlocks = new ArrayList<>();
        int totalLength = 0;
        Block block = Blockchain.getBlock(Convert.parseUnsignedLong((String) request.get("blockId")));
        while (block != null && block.nextBlock != null) {

            block = Blockchain.getBlock(block.nextBlock);
            if (block != null) {

                int length = Nxt.BLOCK_HEADER_LENGTH + block.payloadLength;
                if (totalLength + length > 1048576) {

                    break;

                }

                nextBlocks.add(block);
                totalLength += length;

            }

        }

        JSONArray nextBlocksArray = new JSONArray();
        for (Block nextBlock : nextBlocks) {

            nextBlocksArray.add(nextBlock.getJSONStreamAware());

        }
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

}
