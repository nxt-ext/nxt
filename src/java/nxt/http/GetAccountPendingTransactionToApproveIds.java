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


public class GetAccountPendingTransactionToApproveIds extends APIServlet.APIRequestHandler {

    static final GetAccountPendingTransactionToApproveIds instance = new GetAccountPendingTransactionToApproveIds();

    private GetAccountPendingTransactionToApproveIds() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.PENDING_TRANSACTIONS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactionIdsJson = new JSONArray();
        try (DbIterator<Long> transactionIds = PendingTransactionPoll.getIdsByWhitelistedSigner(account, firstIndex, lastIndex)) {
            for (Long transactionId : transactionIds) {
                transactionIdsJson.add(Convert.toUnsignedLong(transactionId));
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactionIds", transactionIdsJson);
        return response;
    }
}
