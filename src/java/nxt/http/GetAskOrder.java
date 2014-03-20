package nxt.http;

import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ORDER;
import static nxt.http.JSONResponses.MISSING_ORDER;
import static nxt.http.JSONResponses.UNKNOWN_ORDER;

public final class GetAskOrder extends APIServlet.APIRequestHandler {

    static final GetAskOrder instance = new GetAskOrder();

    private GetAskOrder() {
        super("order");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String order = req.getParameter("order");
        if (order == null) {
            return MISSING_ORDER;
        }

        Order.Ask orderData;
        try {
            orderData = Order.Ask.getAskOrder(Convert.parseUnsignedLong(order));
            if (orderData == null) {
                return UNKNOWN_ORDER;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ORDER;
        }

        JSONObject response = new JSONObject();

        response.put("account", Convert.toUnsignedLong(orderData.getAccount().getId()));
        response.put("asset", Convert.toUnsignedLong(orderData.getAssetId()));
        response.put("quantity", orderData.getQuantity());
        response.put("price", orderData.getPrice());
        response.put("height", orderData.getHeight());
        return response;
    }

}
