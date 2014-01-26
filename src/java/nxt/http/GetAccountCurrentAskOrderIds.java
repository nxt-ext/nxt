package nxt.http;

import nxt.Account;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAccountCurrentAskOrderIds extends HttpRequestHandler {

    static final GetAccountCurrentAskOrderIds instance = new GetAccountCurrentAskOrderIds();

    private GetAccountCurrentAskOrderIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String account = req.getParameter("account");
        if (account == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"account\" not specified");

        } else {

            try {

                Account accountData = Account.getAccount(Convert.parseUnsignedLong(account));
                if (accountData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown account");

                } else {

                    boolean assetIsNotUsed = false;
                    Long assetId = null;
                    try {

                        assetId = Convert.parseUnsignedLong(req.getParameter("asset"));

                    } catch (Exception e) {
                        //TODO: why not just return an error?
                        assetIsNotUsed = true;

                    }

                    JSONArray orderIds = new JSONArray();
                    for (Order.Ask askOrder : Order.Ask.allAskOrders) {

                        if ((assetIsNotUsed || askOrder.asset.equals(assetId)) && askOrder.account == accountData) {

                            orderIds.add(Convert.convert(askOrder.id));

                        }

                    }
                    response.put("askOrderIds", orderIds);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"account\"");

            }
        }
        return response;
    }

}
