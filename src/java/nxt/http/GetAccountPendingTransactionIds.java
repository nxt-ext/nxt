package nxt.http;


import nxt.Account;
import nxt.NxtException;
import nxt.PendingTransactionPoll;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public class GetAccountPendingTransactionIds extends APIServlet.APIRequestHandler {

    static final GetAccountPendingTransactionIds instance = new GetAccountPendingTransactionIds();

    private GetAccountPendingTransactionIds() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.PENDING_TRANSACTIONS},
                "account", "finished", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        long accountId = account.getId();
        String finished = Convert.nullToEmpty(req.getParameter("finished")).toLowerCase();
        DbIterator<PendingTransactionPoll> iterator;
        switch (finished) {
            case "true":
                iterator = PendingTransactionPoll.getFinishedByAccountId(accountId, firstIndex, lastIndex);
                break;
            case "false":
                iterator = PendingTransactionPoll.getActiveByAccountId(accountId, firstIndex, lastIndex);
                break;
            default:
                iterator = PendingTransactionPoll.getByAccountId(accountId, firstIndex, lastIndex);
                break;
        }

        JSONArray transactionIds = new JSONArray();
        while (iterator.hasNext()) {
            PendingTransactionPoll pendingTransactionPoll = iterator.next();
            transactionIds.add(Convert.toUnsignedLong(pendingTransactionPoll.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("transactionIds", transactionIds);
        return response;
    }
}
