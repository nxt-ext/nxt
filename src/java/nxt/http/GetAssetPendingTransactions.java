package nxt.http;


import nxt.Asset;
import nxt.PendingTransactionPoll;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAssetPendingTransactions extends APIServlet.APIRequestHandler {
    static final GetAssetPendingTransactions instance = new GetAssetPendingTransactions();

    private GetAssetPendingTransactions() {
        super(new APITag[]{APITag.AE, APITag.PENDING_TRANSACTIONS}, "asset", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Asset asset = ParameterParser.getAsset(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator =
                     PendingTransactionPoll.getPendingTransactionsForAsset(asset, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction));
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;
    }

}
