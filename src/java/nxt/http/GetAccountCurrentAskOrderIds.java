package nxt.http;

import nxt.NxtException;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAccountCurrentAskOrderIds extends APIServlet.APIRequestHandler {

    static final GetAccountCurrentAskOrderIds instance = new GetAccountCurrentAskOrderIds();

    private GetAccountCurrentAskOrderIds() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.AE}, "account", "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long accountId = ParameterParser.getAccount(req).getId();
        Long assetId = null;
        try {
            assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
        } catch (RuntimeException e) {
            // ignore
        }

        List<Order.Ask> askOrders;
        if (assetId == null) {
            askOrders = Order.Ask.getAskOrdersByAccount(accountId);
        } else {
            askOrders = Order.Ask.getAskOrdersByAccountAsset(accountId, assetId);
        }
        JSONArray orderIds = new JSONArray();
        for (Order.Ask askOrder : askOrders) {
            orderIds.add(Convert.toUnsignedLong(askOrder.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("askOrderIds", orderIds);
        return response;
    }

}
