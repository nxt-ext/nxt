package nxt.http;

import nxt.Account;
import nxt.Asset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAssetsByIssuer extends APIServlet.APIRequestHandler {

    static final GetAssetsByIssuer instance = new GetAssetsByIssuer();

    private GetAssetsByIssuer() {
        super("account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Account account= ParameterParser.getAccount(req);
        List<Asset> assets = Asset.getAssetsIssuedBy(account.getId());
        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        for (Asset asset : assets) {
            assetsJSONArray.add(JSONData.asset(asset));
        }
        return response;
    }

}
