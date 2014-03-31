package nxt.http;

import nxt.Account;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.DUPLICATE_AMOUNT;
import static nxt.http.JSONResponses.INCORRECT_AMOUNT;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_AMOUNT;
import static nxt.http.JSONResponses.MISSING_RECIPIENT;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class SendMoney extends CreateTransaction {

    static final SendMoney instance = new SendMoney();

    private SendMoney() {
        super("recipient", "amountNXT", "amountNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String recipientValue = req.getParameter("recipient");
        String amountValueNXT = Convert.emptyToNull(req.getParameter("amountNXT"));
        String amountValueNQT = Convert.emptyToNull(req.getParameter("amountNQT"));


        if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (amountValueNXT == null && amountValueNQT == null) {
            return MISSING_AMOUNT;
        } else if (amountValueNXT != null && amountValueNQT != null) {
            return DUPLICATE_AMOUNT;
        }

        Long recipient;
        try {
            recipient = Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            return INCORRECT_RECIPIENT;
        }

        long amountNQT;
        try {
            amountNQT = amountValueNQT != null ? Long.parseLong(amountValueNQT) : Convert.parseNXT(amountValueNXT);
            if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NXT * Constants.ONE_NXT) {
                return INCORRECT_AMOUNT;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_AMOUNT;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        return createTransaction(req, account, recipient, amountNQT, null);

    }

}
