package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountAssets extends APIServlet.APIRequestHandler {

    static final GetAccountAssets instance = new GetAccountAssets();

    private GetAccountAssets() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.AE}, "account", "asset", "height");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        int height = ParameterParser.getHeight(req);
        String assetValue = Convert.emptyToNull(req.getParameter("asset"));

        if (assetValue == null) {
            JSONObject response = new JSONObject();
            try (DbIterator<Account.AccountAsset> accountAssets = account.getAssets(height, 0, -1)) {
                JSONArray assetJSON = new JSONArray();
                while (accountAssets.hasNext()) {
                    assetJSON.add(JSONData.accountAsset(accountAssets.next(), false, true));
                }
                response.put("accountAssets", assetJSON);
                return response;
            }
        } else {
            Asset asset = ParameterParser.getAsset(req);
            Account.AccountAsset accountAsset = account.getAsset(asset.getId(), height);
            if (accountAsset != null) {
                return JSONData.accountAsset(accountAsset, false, true);
            }
            return JSON.emptyJSON;
        }
    }

}
