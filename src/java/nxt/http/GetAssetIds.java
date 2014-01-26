package nxt.http;

import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAssetIds extends HttpRequestHandler {

    static final GetAssetIds instance = new GetAssetIds();

    private GetAssetIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        JSONArray assetIds = new JSONArray();
        for (Asset asset : Asset.allAssets) {

            assetIds.add(Convert.convert(asset.assetId));

        }
        response.put("assetIds", assetIds);

        return response;
    }

}
