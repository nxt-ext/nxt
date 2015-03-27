package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.PhasingPoll;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccountPendingTransactionCount extends APIServlet.APIRequestHandler {
    static final GetAccountPendingTransactionCount instance = new GetAccountPendingTransactionCount();

    private GetAccountPendingTransactionCount() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.PHASING}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);
        JSONObject response = new JSONObject();
        response.put("numberOfPendingTransactions", PhasingPoll.getAccountPendingTransactionCount(account));
        return response;
    }
}