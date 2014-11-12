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


public class GetAccountPendingTransactionToSignIds extends APIServlet.APIRequestHandler {

    static final GetAccountPendingTransactionToSignIds instance = new GetAccountPendingTransactionToSignIds();

    private GetAccountPendingTransactionToSignIds() {
        super(new APITag[]{APITag.ACCOUNTS}, "account", "finished", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);
        String finishedValue = Convert.nullToEmpty(req.getParameter("finished")).toLowerCase();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        Boolean finished = null;
        switch (finishedValue) {
            case "true":
                finished = true;
                break;
            case "false":
                finished = false;
                break;
            default:
                break;
        }

        List<Long> transactionIds = PendingTransactionPoll.getIdsByWhitelistedSigner(account, finished, firstIndex, lastIndex);


        JSONArray transactionIdsJson = new JSONArray();
        for (Long transactionId : transactionIds) {
            transactionIdsJson.add(Convert.toUnsignedLong(transactionId));
        }
        JSONObject response = new JSONObject();
        response.put("transactionIds", transactionIdsJson);
        return response;
    }
}
