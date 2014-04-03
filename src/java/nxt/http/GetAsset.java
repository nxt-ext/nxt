package nxt.http;

import nxt.Asset;
import nxt.util.Convert;
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

        Asset assetData;
        try {
            assetData = Asset.getAsset(Convert.parseUnsignedLong(asset));
            if (assetData == null) {
                return UNKNOWN_ASSET;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        return JSONData.asset(assetData);

    }

}
