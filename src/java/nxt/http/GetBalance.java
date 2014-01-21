package nxt.http;

import nxt.Account;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetBalance extends HttpRequestHandler {

    static final GetBalance instance = new GetBalance();

    private GetBalance() {}

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

                    response.put("balance", 0);
                    response.put("unconfirmedBalance", 0);
                    response.put("effectiveBalance", 0);

                } else {

                    synchronized (accountData) {

                        response.put("balance", accountData.getBalance());
                        response.put("unconfirmedBalance", accountData.getUnconfirmedBalance());
                        response.put("effectiveBalance", accountData.getEffectiveBalance() * 100L);

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
