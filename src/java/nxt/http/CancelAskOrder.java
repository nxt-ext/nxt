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

public final class CancelAskOrder extends APIServlet.APIRequestHandler {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String secretPhrase = req.getParameter("secretPhrase");
        String orderValue = req.getParameter("order");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");

        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (orderValue == null) {
            return MISSING_ORDER;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        Long order;
        try {
            order = Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            return INCORRECT_ORDER;
        }

        int fee;
        try {
            fee = Integer.parseInt(feeValue);
            if (fee <= 0 || fee >= Nxt.MAX_BALANCE) {
                return INCORRECT_FEE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_FEE;
        }

        short deadline;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1 || deadline > 1440) {
                return INCORRECT_DEADLINE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DEADLINE;
        }

        Long referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        Long accountId = Account.getId(publicKey);

        Order.Ask orderData = Order.Ask.getAskOrder(order);
        if (orderData == null || !orderData.getAccount().getId().equals(accountId)) {
            return UNKNOWN_ORDER;
        }

        Account account = Account.getAccount(accountId);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        int timestamp = Convert.getEpochTime();
        Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
        Transaction transaction = Nxt.getTransactionProcessor().newTransaction(timestamp, deadline,
                publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
        transaction.sign(secretPhrase);

        Nxt.getTransactionProcessor().broadcast(transaction);

        JSONObject response = new JSONObject();
        response.put("transaction", transaction.getStringId());
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
