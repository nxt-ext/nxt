package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.PhasingPoll;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccountPhasedTransactionCount extends APIServlet.APIRequestHandler {
    static final GetAccountPhasedTransactionCount instance = new GetAccountPhasedTransactionCount();

    private GetAccountPhasedTransactionCount() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.PHASING}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);
        JSONObject response = new JSONObject();
        response.put("numberOfPhasedTransactions", PhasingPoll.getAccountPhasedTransactionCount(account));
        return response;
    }
}