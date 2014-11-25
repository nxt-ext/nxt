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
        //TODO: trying to use a boolean parameter to carry three possible values is not a good design,
        // better use a state parameter that can have multiple values, e.g. state=finished, state=pending
        // Then if in the future another state value needs to be added, e.g. state=rejected,
        // you can keep backwards compatibility, which would not be possible if the parameter were a boolean to begin with.
        // Anyway, why is it needed to be able to request finished pending transactions? I would rather not have
        // that ability, which will make it possible to delete entries in the pending_transaction table once finished and keep it small.
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
