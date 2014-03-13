package nxt.http;

import nxt.Account;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_AMOUNT;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_AMOUNT;
import static nxt.http.JSONResponses.MISSING_RECIPIENT;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class SendMoney extends CreateTransaction {

    static final SendMoney instance = new SendMoney();

    private SendMoney() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String recipientValue = req.getParameter("recipient");
        String amountValue = req.getParameter("amount");

        if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (amountValue == null) {
            return MISSING_AMOUNT;
        }

        Long recipient;
        try {
            recipient = Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            return INCORRECT_RECIPIENT;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountValue);
            if (amount <= 0 || amount >= Constants.MAX_BALANCE) {
                return INCORRECT_AMOUNT;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_AMOUNT;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        return createTransaction(req, account, recipient, amount, null);

    }

}
