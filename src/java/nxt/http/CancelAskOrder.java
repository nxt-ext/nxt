package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Order;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class CancelAskOrder extends HttpRequestHandler {

    static final CancelAskOrder instance = new CancelAskOrder();

    private CancelAskOrder() {}

    private static final JSONStreamAware MISSING_SECRET_PHRASE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"secretPhrase\" not specified");
        MISSING_SECRET_PHRASE = JSON.prepare(response);
    }

    private static final JSONStreamAware MISSING_FEE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"fee\" not specified");
        MISSING_FEE = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_FEE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"fee\"");
        INVALID_FEE = JSON.prepare(response);
    }

    private static final JSONStreamAware MISSING_DEADLINE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"deadline\" not specified");
        MISSING_DEADLINE = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_DEADLINE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"deadline\"");
        INVALID_DEADLINE = JSON.prepare(response);
    }

    private static final JSONStreamAware MISSING_ORDER;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"order\" not specified");
        MISSING_ORDER = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_ORDER;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"order\"");
        INVALID_ORDER = JSON.prepare(response);
    }

    private static final JSONStreamAware UNKNOWN_ORDER;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "Unknown order");
        UNKNOWN_ORDER = JSON.prepare(response);
    }

    private static final JSONStreamAware NOT_ENOUGH_FUNDS;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 6);
        response.put("errorDescription", "Not enough funds");
        NOT_ENOUGH_FUNDS = JSON.prepare(response);
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

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
            return INVALID_ORDER;
        }

        int fee;
        try {
            fee = Integer.parseInt(feeValue);
            if (fee <= 0 || fee >= Nxt.MAX_BALANCE) {
                return INVALID_FEE;
            }
        } catch (NumberFormatException e) {
            return INVALID_FEE;
        }

        short deadline;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1) {
                return INVALID_DEADLINE;
            }
        } catch (NumberFormatException e) {
            return INVALID_DEADLINE;
        }

        Long referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        Long accountId = Account.getId(publicKey);

        Order.Ask orderData = Order.Ask.getAskOrder(order);
        if (orderData == null || !orderData.account.id.equals(accountId)) {
            return UNKNOWN_ORDER;
        }

        Account account = Account.getAccount(accountId);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        int timestamp = Convert.getEpochTime();
        Attachment attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
        Transaction transaction = Transaction.newTransaction(timestamp, deadline,
                publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
        transaction.sign(secretPhrase);

        Blockchain.broadcast(transaction);

        JSONObject response = new JSONObject();
        response.put("transaction", transaction.getStringId());
        return response;

    }

}
