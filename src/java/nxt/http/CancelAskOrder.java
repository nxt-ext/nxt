package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Order;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_ORDER;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_ORDER;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;
import static nxt.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelAskOrder extends CreateTransaction {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String orderValue = req.getParameter("order");
        if (orderValue == null) {
            return MISSING_ORDER;
        }

        Long order;
        try {
            order = Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            return INCORRECT_ORDER;
        }

        Account account = getAccount(req);
        if (account == null) {
            return NOT_ENOUGH_FUNDS;
        }

        Order.Ask orderData = Order.Ask.getAskOrder(order);
        if (orderData == null || ! orderData.getAccount().getId().equals(account.getId())) {
            return UNKNOWN_ORDER;
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
        return createTransaction(req, account, attachment);

    }

}
