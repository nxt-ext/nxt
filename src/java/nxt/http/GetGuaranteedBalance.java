package nxt.http;

import nxt.Account;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetGuaranteedBalance extends HttpRequestHandler {

    static final GetGuaranteedBalance instance = new GetGuaranteedBalance();

    private GetGuaranteedBalance() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String account = req.getParameter("account");
        String numberOfConfirmationsValue = req.getParameter("numberOfConfirmations");
        if (account == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"account\" not specified");

        } else if (numberOfConfirmationsValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"numberOfConfirmations\" not specified");

        } else {

            try {

                Account accountData = Nxt.accounts.get(Convert.parseUnsignedLong(account));
                if (accountData == null) {

                    response.put("guaranteedBalance", 0);

                } else {

                    try {

                        int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                        response.put("guaranteedBalance", accountData.getGuaranteedBalance(numberOfConfirmations));

                    } catch (Exception e) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"numberOfConfirmations\"");

                    }

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"account\"");

            }

        }
        return response;
    }

}
