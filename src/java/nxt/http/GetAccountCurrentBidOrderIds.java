package nxt.http;

import nxt.Account;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAccountCurrentBidOrderIds extends HttpRequestHandler {

    static final GetAccountCurrentBidOrderIds instance = new GetAccountCurrentBidOrderIds();

    private GetAccountCurrentBidOrderIds() {}

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

                        assetIsNotUsed = true;

                    }

                    JSONArray orderIds = new JSONArray();
                    for (Order.Bid bidOrder : Order.Bid.allBidOrders) {

                        if ((assetIsNotUsed || bidOrder.asset.equals(assetId)) && bidOrder.account == accountData) {

                            orderIds.add(Convert.convert(bidOrder.id));

                        }

                    }
                    response.put("bidOrderIds", orderIds);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"account\"");

            }
        }
        return response;
    }

}
