package nxt.http;

import nxt.Blockchain;
import nxt.Transaction;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class BroadcastTransaction extends HttpRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {}

    private static final JSONStreamAware MISSING_TRANSACTION_BYTES;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"transactionBytes\" not specified");
        MISSING_TRANSACTION_BYTES = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_TRANSACTION_BYTES;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"transactionBytes\"");
        INVALID_TRANSACTION_BYTES = JSON.prepare(response);
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {
            return MISSING_TRANSACTION_BYTES;
        }

        try {

            ByteBuffer buffer = ByteBuffer.wrap(Convert.convert(transactionBytes));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            Transaction transaction = Transaction.getTransaction(buffer);

            Blockchain.broadcast(transaction);

            JSONObject response = new JSONObject();
            response.put("transaction", transaction.getStringId());
            return response;

        } catch (RuntimeException e) {
            return INVALID_TRANSACTION_BYTES;
        }
    }

}
