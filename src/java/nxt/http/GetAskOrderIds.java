package nxt.http;

import nxt.Asset;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

public final class GetAskOrderIds extends APIServlet.APIRequestHandler {

    static final GetAskOrderIds instance = new GetAskOrderIds();

    private GetAskOrderIds() {
        super("asset", "limit");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String asset = req.getParameter("asset");
        if (asset == null) {
            return MISSING_ASSET;
        }

        long assetId;
        try {
            assetId = Convert.parseUnsignedLong(asset);
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        if (Asset.getAsset(assetId) == null) {
            return UNKNOWN_ASSET;
        }

        int limit;
        try {
            limit = Integer.parseInt(req.getParameter("limit"));
        } catch (NumberFormatException e) {
            limit = Integer.MAX_VALUE;
        }

        JSONArray orderIds = new JSONArray();
        Iterator<Order.Ask> askOrders = Order.Ask.getSortedOrders(assetId).iterator();
        while (askOrders.hasNext() && limit-- > 0) {
            orderIds.add(Convert.toUnsignedLong(askOrders.next().getId()));
        }

        JSONObject response = new JSONObject();
        response.put("askOrderIds", orderIds);
        return response;

    }

}
