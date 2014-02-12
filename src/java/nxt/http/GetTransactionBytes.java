package nxt.http;

import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class GetTransactionBytes extends HttpRequestDispatcher.HttpRequestHandler {

    static final GetTransactionBytes instance = new GetTransactionBytes();

    private GetTransactionBytes() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String transaction = req.getParameter("transaction");
        if (transaction == null) {
            return MISSING_TRANSACTION;
        }

        Long transactionId;
        Transaction transactionData;
        try {
            transactionId = Convert.parseUnsignedLong(transaction);
            transactionData = Blockchain.getTransaction(transactionId);
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        JSONObject response = new JSONObject();
        if (transactionData == null) {
            transactionData = Blockchain.getUnconfirmedTransaction(transactionId);
            if (transactionData == null) {
                return UNKNOWN_TRANSACTION;
            } else {
                response.put("bytes", Convert.convert(transactionData.getBytes()));
            }
        } else {
            response.put("bytes", Convert.convert(transactionData.getBytes()));
            Block block = transactionData.getBlock();
            response.put("confirmations", Blockchain.getLastBlock().getHeight() - block.getHeight() + 1);

        }
        return response;
    }

}
