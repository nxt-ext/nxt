package nxt.http;


import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS;
import static nxt.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_URI_LENGTH;
import static nxt.http.JSONResponses.MISSING_ALIAS;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.MISSING_URI;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

final class AssignAlias extends HttpRequestHandler {

    static final AssignAlias instance = new AssignAlias();

    private AssignAlias() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationFailure {
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
            return INCORRECT_ALIAS_LENGTH;
        }

        String normalizedAlias = alias.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INCORRECT_ALIAS;
            }
        }

        uri = uri.trim();
        if (uri.length() > Nxt.MAX_ALIAS_URI_LENGTH) {
            return INCORRECT_URI_LENGTH;
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
            if (deadline < 1) {
                return INCORRECT_DEADLINE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DEADLINE;
        }

        Long referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        Account account = Account.getAccount(publicKey);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        Alias aliasData = Alias.getAlias(normalizedAlias);
        JSONObject response = new JSONObject();
        if (aliasData != null && aliasData.getAccount() != account) {

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
