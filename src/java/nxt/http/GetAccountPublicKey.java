package nxt.http;

import nxt.Account;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccountPublicKey extends APIServlet.APIRequestHandler {

    static final GetAccountPublicKey instance = new GetAccountPublicKey();

    private GetAccountPublicKey() {
        super("account");
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
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        if (account.getPublicKey() != null) {

            JSONObject response = new JSONObject();
            response.put("publicKey", Convert.toHexString(account.getPublicKey()));
            return response;

        } else {
            return JSON.emptyJSON;
        }
    }

}
