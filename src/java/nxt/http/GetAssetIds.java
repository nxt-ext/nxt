package nxt.http;

import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class GetAssetIds extends HttpRequestHandler {

    static final GetAssetIds instance = new GetAssetIds();

    private GetAssetIds() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray assetIds = new JSONArray();
        for (Asset asset : Asset.allAssets) {
            assetIds.add(Convert.convert(asset.assetId));
        }

        JSONObject response = new JSONObject();
        response.put("assetIds", assetIds);
        return response;
    }

}
