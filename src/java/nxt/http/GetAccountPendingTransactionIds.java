package nxt.http;


import nxt.Account;
import nxt.NxtException;
import nxt.PendingTransactionPoll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public class GetAccountPendingTransactionIds extends APIServlet.APIRequestHandler {

    static final GetAccountPendingTransactionIds instance = new GetAccountPendingTransactionIds();

    private GetAccountPendingTransactionIds() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.PENDING_TRANSACTIONS},
                "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        long accountId = account.getId();
        List<PendingTransactionPoll> polls = PendingTransactionPoll.getByAccountId(accountId, firstIndex, lastIndex).toList();

        JSONArray transactionIds = new JSONArray();
        for (PendingTransactionPoll poll:polls) {
            transactionIds.add(Convert.toUnsignedLong(poll.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("transactionIds", transactionIds);
        return response;
    }
}
