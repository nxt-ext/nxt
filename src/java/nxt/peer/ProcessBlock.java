package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ProcessBlock extends HttpJSONRequestHandler {

    static final ProcessBlock instance = new ProcessBlock();

    private ProcessBlock() {}


    @Override
    public JSONObject processJSONRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

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
        response.put("accepted", accepted);

        return response;
    }

}
