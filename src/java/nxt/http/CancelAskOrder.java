package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ORDER;
import static nxt.http.JSONResponses.MISSING_ORDER;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;
import static nxt.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelAskOrder extends CreateTransaction {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {
        super("order");
    }

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
            return UNKNOWN_ACCOUNT;
        }

        Order.Ask orderData = Order.Ask.getAskOrder(order);
        if (orderData == null || ! orderData.getAccount().getId().equals(account.getId())) {
            return UNKNOWN_ORDER;
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
        return createTransaction(req, account, attachment);

    }

}
