package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccountPendingTransactionToSignIds extends APIServlet.APIRequestHandler {

    static final GetAccountPendingTransactionToSignIds instance = new GetAccountPendingTransactionToSignIds();

    private GetAccountPendingTransactionToSignIds() {
        super(new APITag[]{APITag.ACCOUNTS}, "account", "finished", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        JSONObject response = new JSONObject();
        //todo: finish
        return response;
    }
}
