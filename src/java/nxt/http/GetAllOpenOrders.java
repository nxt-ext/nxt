package nxt.http;

import nxt.Order;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import nxt.util.Convert;

public final class GetAllOpenOrders extends APIServlet.APIRequestHandler {

    static final GetAllOpenOrders instance = new GetAllOpenOrders();

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray ordersData = new JSONArray();

        try {
            Collection<Order.Ask> askOrders = Order.Ask.getAllAskOrders();
            Collection<Order.Bid> bidOrders = Order.Bid.getAllBidOrders();

            for (Order.Ask order : askOrders) {
                JSONObject orderData = new JSONObject();
                orderData.put("type", "ask");
                orderData.put("assetId", Convert.toUnsignedLong(order.getAssetId()));
                orderData.put("accountId", Convert.toUnsignedLong(order.getAccount().getId()));
                orderData.put("quantity", order.getQuantity());
                orderData.put("price", order.getPrice());
                orderData.put("height", order.getHeight());
                ordersData.add(orderData);
            }
            for (Order.Bid order : bidOrders) {
                JSONObject orderData = new JSONObject();
                orderData.put("type", "bid");
                orderData.put("assetId", Convert.toUnsignedLong(order.getAssetId()));
                orderData.put("accountId", Convert.toUnsignedLong(order.getAccount().getId()));
                orderData.put("quantity", order.getQuantity());
                orderData.put("price", order.getPrice());
                orderData.put("height", order.getHeight());
                ordersData.add(orderData);
            }

        } catch (RuntimeException e) {
            response.put("error", e.toString());
        }

        response.put("openOrders", ordersData);
        return response;
    }

}
