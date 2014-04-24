package nxt.http;

import nxt.Asset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.MISSING_ASSET_NAME;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

public final class GetAllAssets extends APIServlet.APIRequestHandler {

    static final GetAllAssets instance = new GetAllAssets();

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        for (Asset asset : Asset.getAllAssets()) {
            assetsJSONArray.add(JSONData.asset(asset));
        }
        return response;
    }

}
