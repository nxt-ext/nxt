package nxt.http;

import nxt.Account;
import nxt.AskOrder;
import nxt.Blockchain;
import nxt.Nxt;
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

                Account accountData = Nxt.accounts.get(Convert.parseUnsignedLong(account));
                if (accountData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown account");

                } else {

                    boolean assetIsNotUsed;
                    long assetId;
                    try {

                        assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
                        assetIsNotUsed = false;

                    } catch (Exception e) {

                        assetId = 0;
                        assetIsNotUsed = true;

                    }

                    JSONArray orderIds = new JSONArray();
                    for (AskOrder askOrder : Blockchain.askOrders.values()) {

                        if ((assetIsNotUsed || askOrder.asset == assetId) && askOrder.account == accountData) {

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
