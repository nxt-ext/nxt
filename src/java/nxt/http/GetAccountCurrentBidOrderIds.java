package nxt.http;

import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAccountCurrentBidOrderIds extends APIServlet.APIRequestHandler {

    static final GetAccountCurrentBidOrderIds instance = new GetAccountCurrentBidOrderIds();

    private GetAccountCurrentBidOrderIds() {
        super("account", "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Long accountId = ParameterParser.getAccount(req).getId();
        Long assetId = null;
        try {
            assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
        } catch (RuntimeException e) {
            // ignore
        }

        List<Order.Bid> bidOrders;
        if (assetId == null) {
            bidOrders = Order.Bid.getBidOrdersByAccount(accountId);
        } else {
            bidOrders = Order.Bid.getBidOrdersByAccountAsset(accountId, assetId);
        }
        JSONArray orderIds = new JSONArray();
        for (Order.Bid bidOrder : bidOrders) {
            orderIds.add(Convert.toUnsignedLong(bidOrder.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("bidOrderIds", orderIds);
        return response;
    }

}