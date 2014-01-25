package nxt.http;

import nxt.Blockchain;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetUnconfirmedTransactionIds extends HttpRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        JSONArray transactionIds = new JSONArray();
        for (Transaction transaction : Blockchain.allUnconfirmedTransactions) {

            transactionIds.add(transaction.getStringId());

        }
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
