package nxt.http;

import nxt.Account;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_NUMBER_OF_CONFIRMATIONS;

public final class GetGuaranteedBalance extends APIServlet.APIRequestHandler {

    static final GetGuaranteedBalance instance = new GetGuaranteedBalance();

    private GetGuaranteedBalance() {
        super("account", "numberOfConfirmations");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String account = req.getParameter("account");
        String numberOfConfirmationsValue = req.getParameter("numberOfConfirmations");
        if (account == null) {
            return MISSING_ACCOUNT;
        } else if (numberOfConfirmationsValue == null) {
            return MISSING_NUMBER_OF_CONFIRMATIONS;
        }

        Account accountData;
        try {
            accountData = Account.getAccount(Convert.parseUnsignedLong(account));
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        JSONObject response = new JSONObject();
        if (accountData == null) {
            response.put("guaranteedBalance", 0);
        } else {
            try {
                int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                response.put("guaranteedBalance", accountData.getGuaranteedBalance(numberOfConfirmations));
            } catch (NumberFormatException e) {
                return INCORRECT_NUMBER_OF_CONFIRMATIONS;
            }
        }

        return response;
    }

}
