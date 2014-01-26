package nxt.http;

import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAskOrderIds extends HttpRequestHandler {

    static final GetAskOrderIds instance = new GetAskOrderIds();

    private GetAskOrderIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        JSONArray orderIds = new JSONArray();
        for (Order.Ask order : Order.Ask.allAskOrders) {

            orderIds.add(Convert.convert(order.id));

        }
        response.put("askOrderIds", orderIds);
        return response;
    }

}
