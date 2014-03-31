package nxt.http;

import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

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
                orderData.put("asset", Convert.toUnsignedLong(order.getAssetId()));
                orderData.put("account", Convert.toUnsignedLong(order.getAccount().getId()));
                orderData.put("quantity", order.getQuantity());
                orderData.put("priceNQT", order.getPriceNQT());
                orderData.put("height", order.getHeight());
                ordersData.add(orderData);
            }
            for (Order.Bid order : bidOrders) {
                JSONObject orderData = new JSONObject();
                orderData.put("type", "bid");
                orderData.put("asset", Convert.toUnsignedLong(order.getAssetId()));
                orderData.put("account", Convert.toUnsignedLong(order.getAccount().getId()));
                orderData.put("quantity", order.getQuantity());
                orderData.put("priceNQT", order.getPriceNQT());
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
