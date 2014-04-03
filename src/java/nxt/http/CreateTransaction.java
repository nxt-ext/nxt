package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static nxt.http.JSONResponses.DUPLICATE_FEE;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + 5);
        System.arraycopy(new String[]{"secretPhrase", "publicKey", "feeNXT", "feeNQT", "deadline", "referencedTransaction"}, 0,
                result, parameters.length, 5);
        return result;
    }

    CreateTransaction(String... parameters) {
        super(addCommonParameters(parameters));
    }

    final Account getAccount(HttpServletRequest req) {
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase != null) {
            return Account.getAccount(Crypto.getPublicKey(secretPhrase));
        }
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (publicKeyString == null) {
            return null;
        }
        try {
            return Account.getAccount(Convert.parseHexString(publicKeyString));
        } catch (RuntimeException e) {
            return null;
        }
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
        throws NxtException.ValidationException {
        return createTransaction(req, senderAccount, Genesis.CREATOR_ID, 0, attachment);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId,
                                            long amountNQT, Attachment attachment) throws NxtException.ValidationException {
        String deadlineValue = req.getParameter("deadline");
        String feeValueNXT = Convert.emptyToNull(req.getParameter("feeNXT"));
        String feeValueNQT = Convert.emptyToNull(req.getParameter("feeNQT"));
        String referencedTransactionValue = Convert.emptyToNull(req.getParameter("referencedTransaction"));
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));

        if (secretPhrase == null && publicKeyValue == null) {
            return MISSING_SECRET_PHRASE;
        } else if (feeValueNXT == null && feeValueNQT == null) {
            return MISSING_FEE;
        } else if (feeValueNXT != null && feeValueNQT != null) {
            return DUPLICATE_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
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

        long feeNQT;
        try {
            feeNQT = feeValueNQT != null ? Long.parseLong(feeValueNQT) : Convert.parseNXT(feeValueNXT);
            if (feeNQT < minimumFeeNQT() || feeNQT >= Constants.MAX_BALANCE_NXT * Constants.ONE_NXT) {
                return INCORRECT_FEE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_FEE;
        }

        try {
            if (Convert.safeAdd(amountNQT, feeNQT) > senderAccount.getUnconfirmedBalanceNQT()) {
                return NOT_ENOUGH_FUNDS;
            }
        } catch (ArithmeticException e) {
            return NOT_ENOUGH_FUNDS;
        }

        Long referencedTransaction;
        try {
            referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);
        } catch (RuntimeException e) {
            return INCORRECT_REFERENCED_TRANSACTION;
        }

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);

        Transaction transaction = attachment == null ?
                Nxt.getTransactionProcessor().newTransaction(deadline, publicKey, recipientId,
                        amountNQT, feeNQT, referencedTransaction)
                :
                Nxt.getTransactionProcessor().newTransaction(deadline, publicKey, recipientId,
                        amountNQT, feeNQT, referencedTransaction, attachment);

        JSONObject response = new JSONObject();
        try {
            if (secretPhrase != null) {
                transaction.sign(secretPhrase);
                Nxt.getTransactionProcessor().broadcast(transaction);
                response.put("transaction", transaction.getStringId());

            }
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            response.put("hash", transaction.getHash());
        } catch (NxtException.ValidationException e) {
            response.put("error", e.getMessage());
        }
        return response;

    }

    @Override
    final boolean requirePost() {
        return true;
    }

    long minimumFeeNQT() {
        return Constants.ONE_NXT;
    }

}
