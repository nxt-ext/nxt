package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Poll;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_POLL;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.INCORRECT_VOTE;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_POLL;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class CastVote extends APIServlet.APIRequestHandler {

    static final CastVote instance = new CastVote();

    private CastVote() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException, IOException {

        String secretPhrase = req.getParameter("secretPhrase");
        String pollValue = req.getParameter("poll");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (pollValue == null) {
            return MISSING_POLL;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        Poll pollData;
        int numberOfOptions = 0;
        try {
            pollData = Poll.getPoll(Convert.parseUnsignedLong(pollValue));
            if (pollData != null) {
                numberOfOptions = pollData.getOptions().length;
            }
        } catch (RuntimeException e) {
            return INCORRECT_POLL;
        }

        byte[] vote = new byte[numberOfOptions];
        try {
            for (int i = 0; i < numberOfOptions; i++) {
                String voteValue = req.getParameter("vote" + i);
                if (voteValue != null) {
                    vote[i] = Byte.parseByte(voteValue);
                }
            }
        } catch (NumberFormatException e) {
            return INCORRECT_VOTE;
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

        Long referencedTransaction;
        try {
            referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);
        } catch (RuntimeException e) {
            return INCORRECT_REFERENCED_TRANSACTION;
        }

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        Account account = Account.getAccount(publicKey);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }
        int timestamp = Convert.getEpochTime();

        Attachment attachment = new Attachment.MessagingVoteCasting(pollData.getId(), vote);
        Transaction transaction = Nxt.getTransactionProcessor().newTransaction(timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
        transaction.sign(secretPhrase);

        Nxt.getTransactionProcessor().broadcast(transaction);

        JSONObject response = new JSONObject();
        response.put("transaction", transaction.getStringId());
        response.put("bytes", Convert.toHexString(transaction.getBytes()));

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
