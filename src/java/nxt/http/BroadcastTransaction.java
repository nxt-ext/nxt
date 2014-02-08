package nxt.http;

import nxt.Blockchain;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION_BYTES;
import static nxt.http.JSONResponses.MISSING_TRANSACTION_BYTES;

public final class BroadcastTransaction extends HttpRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {
            return MISSING_TRANSACTION_BYTES;
        }

        try {

            byte[] bytes = Convert.convert(transactionBytes);
            Transaction transaction = Transaction.getTransaction(bytes);

            Blockchain.broadcast(transaction);

            JSONObject response = new JSONObject();
            response.put("transaction", transaction.getStringId());
            return response;

        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION_BYTES;
        }
    }

}
