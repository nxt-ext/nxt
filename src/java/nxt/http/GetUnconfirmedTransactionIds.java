package nxt.http;

import nxt.Nxt;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {}

    @Override
    List<String> getParameters() {
        return Collections.emptyList();
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray transactionIds = new JSONArray();
        for (Transaction transaction : Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            transactionIds.add(transaction.getStringId());
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
