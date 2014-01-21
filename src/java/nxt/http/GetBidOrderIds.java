package nxt.http;

import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetBidOrderIds extends HttpRequestHandler {

    static final GetBidOrderIds instance = new GetBidOrderIds();

    private GetBidOrderIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        JSONArray orderIds = new JSONArray();
        for (Long orderId : Blockchain.bidOrders.keySet()) {

            orderIds.add(Convert.convert(orderId));

        }
        response.put("bidOrderIds", orderIds);
        return response;
    }

}
