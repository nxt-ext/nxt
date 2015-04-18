package nxt.http;

import nxt.Nxt;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllLostTransactions extends APIServlet.APIRequestHandler {

    static final GetAllLostTransactions instance = new GetAllLostTransactions();

    private GetAllLostTransactions() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("transactions", jsonArray);
        Transaction[] transactions = Nxt.getTransactionProcessor().getAllLostTransactions();
        for (Transaction transaction : transactions) {
            jsonArray.add(JSONData.unconfirmedTransaction(transaction));
        }
        return response;
    }

}
