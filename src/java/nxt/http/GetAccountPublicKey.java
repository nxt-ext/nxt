package nxt.http;

import nxt.Account;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAccountPublicKey extends HttpRequestHandler {

    static final GetAccountPublicKey instance = new GetAccountPublicKey();

    private GetAccountPublicKey() {}

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

                    if (accountData.publicKey.get() != null) {

                        response.put("publicKey", Convert.convert(accountData.publicKey.get()));

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
