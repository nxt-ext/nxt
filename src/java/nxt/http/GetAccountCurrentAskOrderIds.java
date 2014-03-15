package nxt.http;

import nxt.Account;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccountCurrentAskOrderIds extends APIServlet.APIRequestHandler {

    static final GetAccountCurrentAskOrderIds instance = new GetAccountCurrentAskOrderIds();

    private GetAccountCurrentAskOrderIds() {
        super("account", "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String accountId = req.getParameter("account");
        if (accountId == null) {
            return MISSING_ACCOUNT;
        }

        Account account;
        try {
            account = Account.getAccount(Convert.parseUnsignedLong(accountId));
            if (account == null) {
                return UNKNOWN_ACCOUNT;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        Long assetId = null;
        try {
            assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
        } catch (RuntimeException e) {
            // ignore
        }

        JSONArray orderIds = new JSONArray();
        for (Order.Ask askOrder : Order.Ask.getAllAskOrders()) {
            if ((assetId == null || askOrder.getAssetId().equals(assetId)) && askOrder.getAccount().equals(account)) {
                orderIds.add(Convert.toUnsignedLong(askOrder.getId()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("askOrderIds", orderIds);
        return response;
    }

}
