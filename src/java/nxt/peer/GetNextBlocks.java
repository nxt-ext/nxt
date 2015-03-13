package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import nxt.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

final class GetNextBlocks extends PeerServlet.PeerRequestHandler {

    static final GetNextBlocks instance = new GetNextBlocks();

    private GetNextBlocks() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        JSONArray nextBlocksArray = new JSONArray();

        long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<? extends Block> blocks = Nxt.getBlockchain().getBlocksAfter(blockId, 720);

        List<Future<JSONObject>> futures = new ArrayList<>();
        for (Block block : blocks) {
            futures.add(ThreadPool.submit(block::getJSONObject));
        }
        try {
            for (Future<JSONObject> future : futures) {
                nextBlocksArray.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

}
