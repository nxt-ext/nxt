package nxt.http;

import nxt.Blockchain;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class GetUnconfirmedTransactionIds extends HttpRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray transactionIds = new JSONArray();
        for (Transaction transaction : Blockchain.getAllUnconfirmedTransactions()) {
            transactionIds.add(transaction.getStringId());
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
