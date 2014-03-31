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

            response.put("balanceNQT", 0);
            response.put("unconfirmedBalanceNQT", 0);
            response.put("effectiveBalanceNXT", 0);

        } else {

            synchronized (accountData) {
                response.put("balanceNQT", accountData.getBalanceNQT());
                response.put("unconfirmedBalanceNQT", accountData.getUnconfirmedBalanceNQT());
                response.put("effectiveBalanceNXT", accountData.getEffectiveBalanceNXT());
            }

        }
        return response;
    }

}
