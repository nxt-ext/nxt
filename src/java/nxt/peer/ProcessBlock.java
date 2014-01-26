package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ProcessBlock extends HttpJSONRequestHandler {

    static final ProcessBlock instance = new ProcessBlock();

    private ProcessBlock() {}

    private static final JSONStreamAware ACCEPTED;
    static {
        JSONObject response = new JSONObject();
        response.put("accepted", true);
        ACCEPTED = JSON.prepare(response);
    }

    private static final JSONStreamAware NOT_ACCEPTED;
    static {
        JSONObject response = new JSONObject();
        response.put("accepted", false);
        NOT_ACCEPTED = JSON.prepare(response);
    }

    @Override
    public JSONStreamAware processJSONRequest(JSONObject request, Peer peer) {

        boolean accepted;

        Block block = Block.getBlock(request);

        if (block == null) {

            accepted = false;
            if (peer != null) {
                peer.blacklist();
            }

        } else {

            ByteBuffer buffer = ByteBuffer.allocate(Nxt.BLOCK_HEADER_LENGTH + block.payloadLength);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.put(block.getBytes());

            JSONArray transactionsData = (JSONArray)request.get("transactions");
            for (Object transaction : transactionsData) {

                buffer.put(Transaction.getTransaction((JSONObject) transaction).getBytes());

            }

            accepted = Blockchain.pushBlock(buffer, true);

        }

        return accepted ? ACCEPTED : NOT_ACCEPTED;
    }

}
