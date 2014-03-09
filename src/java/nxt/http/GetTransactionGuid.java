package nxt.http;

import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public class GetTransactionGuid extends APIServlet.APIRequestHandler {

    static final GetTransactionGuid instance = new GetTransactionGuid();

    private GetTransactionGuid() {}

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

        if (transactionData == null) {
            transactionData = Nxt.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transactionData == null) {
                return UNKNOWN_TRANSACTION;
            }
        }

        JSONObject response = new JSONObject();
        response.put("guid", Convert.toHexString(transactionData.getGuid()));
        return response;

    }

}
