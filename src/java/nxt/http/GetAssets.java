package nxt.http;

import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssets extends APIServlet.APIRequestHandler {

    static final GetAssets instance = new GetAssets();

    private GetAssets() {
        super("assets", "assets", "assets"); // limit to 3 for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String[] assets = req.getParameterValues("assets");

        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        for (String assetIdString : assets) {
            if (assetIdString == null || assetIdString.equals("")) {
                continue;
            }
            Asset asset = null;
            try {
                Long assetId = Convert.parseUnsignedLong(assetIdString);
                asset = Asset.getAsset(assetId);
            } catch (RuntimeException ignore) {}
            if (asset != null) {
                assetsJSONArray.add(JSONData.asset(asset));
            }
        }
        return response;
    }

}
