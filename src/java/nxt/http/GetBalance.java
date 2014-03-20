package nxt.http;

import nxt.Account;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;

public final class GetBalance extends APIServlet.APIRequestHandler {

    static final GetBalance instance = new GetBalance();

    private GetBalance() {
        super("account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String account = req.getParameter("account");
        if (account == null) {
            return MISSING_ACCOUNT;
        }

        Account accountData;
        try {
            accountData = Account.getAccount(Convert.parseUnsignedLong(account));
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        JSONObject response = new JSONObject();
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
        return response;
    }

}
