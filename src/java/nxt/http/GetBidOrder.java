package nxt.http;

import nxt.BidOrder;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetBidOrder extends HttpRequestHandler {

    static final GetBidOrder instance = new GetBidOrder();

    private GetBidOrder() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String order = req.getParameter("order");
        if (order == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"order\" not specified");

        } else {

            try {

                BidOrder orderData = Blockchain.bidOrders.get(Convert.parseUnsignedLong(order));
                if (orderData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown bid order");

                } else {

                    response.put("account", Convert.convert(orderData.account.id));
                    response.put("asset", Convert.convert(orderData.asset));
                    response.put("quantity", orderData.quantity);
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
