package nxt.http;

import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAskOrder extends HttpRequestHandler {

    static final GetAskOrder instance = new GetAskOrder();

    private GetAskOrder() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String order = req.getParameter("order");
        if (order == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"order\" not specified");

        } else {

            try {

                Order.Ask orderData = Order.Ask.getAskOrder(Convert.parseUnsignedLong(order));
                if (orderData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown ask order");

                } else {

                    response.put("account", Convert.convert(orderData.account.id));
                    response.put("asset", Convert.convert(orderData.asset));
                    response.put("quantity", orderData.getQuantity());
                    response.put("price", orderData.price);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"order\"");

            }

        }
        return response;
    }

}
