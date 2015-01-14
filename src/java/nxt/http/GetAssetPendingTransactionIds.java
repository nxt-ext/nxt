package nxt.http;


import nxt.Asset;
import nxt.PendingTransactionPoll;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAssetPendingTransactionIds extends APIServlet.APIRequestHandler {
    static final GetAssetPendingTransactionIds instance = new GetAssetPendingTransactionIds();

    private GetAssetPendingTransactionIds() {
        super(new APITag[]{APITag.AE, APITag.PENDING_TRANSACTIONS}, "asset", "finished", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Asset asset = ParameterParser.getAsset(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        DbIterator<PendingTransactionPoll> iterator = PendingTransactionPoll.getByAssetId(asset.getId(), firstIndex, lastIndex);

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
