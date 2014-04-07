package nxt.http;

import nxt.Asset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.MISSING_ASSET_NAME;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

public final class GetAssetsByName extends APIServlet.APIRequestHandler {

    static final GetAssetsByName instance = new GetAssetsByName();

    private GetAssetsByName() {
        super("assetName");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String assetName = req.getParameter("assetName");
        if (assetName == null) {
            return MISSING_ASSET_NAME;
        }

        List<Asset> assets = Asset.getAssets(assetName.toLowerCase());
        if (assets == null || assets.isEmpty()) {
            return UNKNOWN_ASSET;
        }

        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        for (Asset asset : assets) {
            assetsJSONArray.add(JSONData.asset(asset));
        }
        return response;
    }

}
