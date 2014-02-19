package nxt.http;

import nxt.TransactionProcessor;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray transactionIds = new JSONArray();
        for (Transaction transaction : TransactionProcessor.getAllUnconfirmedTransactions()) {
            transactionIds.add(transaction.getStringId());
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
