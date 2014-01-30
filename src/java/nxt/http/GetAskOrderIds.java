package nxt.http;

import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class GetAskOrderIds extends HttpRequestHandler {

    static final GetAskOrderIds instance = new GetAskOrderIds();

    private GetAskOrderIds() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray orderIds = new JSONArray();
        for (Order.Ask order : Order.Ask.allAskOrders) {
            orderIds.add(Convert.convert(order.id));
        }

        JSONObject response = new JSONObject();
        response.put("askOrderIds", orderIds);
        return response;

    }

}
