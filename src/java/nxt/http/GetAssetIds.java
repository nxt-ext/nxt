package nxt.http;

import nxt.Nxt;
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
        for (Long assetId : Nxt.assets.keySet()) {

            assetIds.add(Convert.convert(assetId));

        }
        response.put("assetIds", assetIds);

        return response;
    }

}
