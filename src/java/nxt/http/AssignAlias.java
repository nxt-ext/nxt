package nxt.http;


import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

final class AssignAlias extends HttpRequestHandler {

    static final AssignAlias instance = new AssignAlias();

    private AssignAlias() {}

    private static final JSONStreamAware MISSING_SECRET_PHRASE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"secretPhrase\" not specified");
        MISSING_SECRET_PHRASE = JSON.prepare(response);
    }

    private static final JSONStreamAware MISSING_ALIAS;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"alias\" not specified");
        MISSING_ALIAS = JSON.prepare(response);
    }

    private static final JSONStreamAware MISSING_URI;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"uri\" not specified");
        MISSING_URI = JSON.prepare(response);
    }

    private static final JSONStreamAware MISSING_FEE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        response.put("errorDescription", "\"fee\" not specified");
        MISSING_FEE = JSON.prepare(response);
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

    private static final JSONStreamAware INVALID_FEE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"fee\"");
        INVALID_FEE = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_ALIAS_LENGTH;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"alias\" (length must be in [1..100] range)");
        INVALID_ALIAS_LENGTH = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_ALIAS;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"alias\" (must contain only digits and latin letters)");
        INVALID_ALIAS = JSON.prepare(response);
    }

    private static final JSONStreamAware INVALID_URI_LENGTH;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"uri\" (length must be not longer than 1000 characters)");
        INVALID_URI_LENGTH = JSON.prepare(response);
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
        String alias = req.getParameter("alias");
        String uri = req.getParameter("uri");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");

        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (alias == null) {
            return MISSING_ALIAS;
        } else if (uri == null) {
            return MISSING_URI;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        alias = alias.trim();
        if (alias.length() == 0 || alias.length() > Nxt.MAX_ALIAS_LENGTH) {
            return INVALID_ALIAS_LENGTH;
        }

        String normalizedAlias = alias.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INVALID_ALIAS;
            }
        }

        uri = uri.trim();
        if (uri.length() > Nxt.MAX_ALIAS_URI_LENGTH) {
            return INVALID_URI_LENGTH;
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
        Account account = Account.getAccount(publicKey);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        Alias aliasData = Alias.getAlias(normalizedAlias);
        JSONObject response = new JSONObject();
        if (aliasData != null && aliasData.account != account) {

            response.put("errorCode", 8);
            response.put("errorDescription", "\"" + alias + "\" is already used");

        } else {

            int timestamp = Convert.getEpochTime();
            Attachment attachment = new Attachment.MessagingAliasAssignment(alias, uri);
            Transaction transaction = Transaction.newTransaction(timestamp, deadline,
                    publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
            transaction.sign(secretPhrase);

            Blockchain.broadcast(transaction);

            response.put("transaction", transaction.getStringId());

        }

        return response;
    }

}
