package nxt.http;

import nxt.Peer;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class BroadcastTransaction extends HttpRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"transactionBytes\" not specified");

        } else {

            try {

                ByteBuffer buffer = ByteBuffer.wrap(Convert.convert(transactionBytes));
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                Transaction transaction = Transaction.getTransaction(buffer);

                JSONObject peerRequest = new JSONObject();
                peerRequest.put("requestType", "processTransactions");
                JSONArray transactionsData = new JSONArray();
                transactionsData.add(transaction.getJSONObject());
                peerRequest.put("transactions", transactionsData);

                Peer.sendToSomePeers(peerRequest);

                response.put("transaction", transaction.getStringId());

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"transactionBytes\"");

            }

        }
        return response;
    }

}
