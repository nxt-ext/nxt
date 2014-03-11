package nxt.http;

import nxt.Asset;
import nxt.Trade;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

public final class GetAsset extends APIServlet.APIRequestHandler {

    static final GetAsset instance = new GetAsset();

    private GetAsset() {
        super("asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String asset = req.getParameter("asset");
        if (asset == null) {
            return MISSING_ASSET;
        }

        Long assetId;
        Asset assetData;
        try {
            assetData = Asset.getAsset(assetId = Convert.parseUnsignedLong(asset));
            if (assetData == null) {
                return UNKNOWN_ASSET;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        JSONObject response = new JSONObject();
        response.put("account", Convert.toUnsignedLong(assetData.getAccountId()));
        response.put("name", assetData.getName());
        if (assetData.getDescription().length() > 0) {
            response.put("description", assetData.getDescription());
        }
        response.put("quantity", assetData.getQuantity());
        response.put("numberOfTrades", Trade.getTrades(assetId).size());

        return response;
    }

}
