package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.NxtException;
import nxt.Transfer;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTransfers extends APIServlet.APIRequestHandler {

    static final GetTransfers instance = new GetTransfers();

    private GetTransfers() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String assetId = Convert.emptyToNull(req.getParameter("asset"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        DbIterator<Transfer> transfers = null;
        try {
            if (accountId == null) {
                Asset asset = ParameterParser.getAsset(req);
                transfers = asset.getTransfers(firstIndex, lastIndex);
            } else if (assetId == null) {
                Account account = ParameterParser.getAccount(req);
                transfers = account.getTransfers(firstIndex, lastIndex);
            } else {
                Asset asset = ParameterParser.getAsset(req);
                Account account = ParameterParser.getAccount(req);
                transfers = Transfer.getAccountAssetTransfers(account.getId(), asset.getId(), firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                transfersData.add(JSONData.transfer(transfers.next()));
            }
        } finally {
            DbUtils.close(transfers);
        }
        response.put("transfers", transfersData);

        return response;
    }

}
