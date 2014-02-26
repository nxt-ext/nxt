package nxt.http;

import nxt.Block;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class GetTransaction extends APIServlet.APIRequestHandler {

    static final GetTransaction instance = new GetTransaction();

    private GetTransaction() {}

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
            transactionData = Nxt.getBlockchain().getTransaction(transactionId);
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        JSONObject response;
        if (transactionData == null) {
            transactionData = Nxt.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transactionData == null) {
                return UNKNOWN_TRANSACTION;
            } else {
                response = transactionData.getJSONObject();
                response.put("sender", Convert.toUnsignedLong(transactionData.getSenderId()));
            }
        } else {
            response = transactionData.getJSONObject();
            response.put("sender", Convert.toUnsignedLong(transactionData.getSenderId()));
            Block block = transactionData.getBlock();
            response.put("block", block.getStringId());
            response.put("confirmations", Nxt.getBlockchain().getLastBlock().getHeight() - block.getHeight());
        }

        return response;
    }

}
