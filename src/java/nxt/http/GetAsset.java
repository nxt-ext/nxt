package nxt.http;

import nxt.Asset;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAsset extends HttpRequestHandler {

    static final GetAsset instance = new GetAsset();

    private GetAsset() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String asset = req.getParameter("asset");
        if (asset == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"asset\" not specified");

        } else {

            try {

                Asset assetData = Nxt.assets.get(Convert.parseUnsignedLong(asset));
                if (assetData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown asset");

                } else {

                    response.put("account", Convert.convert(assetData.accountId));
                    response.put("name", assetData.name);
                    if (assetData.description.length() > 0) {

                        response.put("description", assetData.description);

                    }
                    response.put("quantity", assetData.quantity);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"asset\"");

            }

        }

        return response;
    }

}
