package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_QUANTITY;
import static nxt.http.JSONResponses.MISSING_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class TransferAsset extends APIServlet.APIRequestHandler {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String secretPhrase = req.getParameter("secretPhrase");
        String recipientValue = req.getParameter("recipient");
        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (assetValue == null) {
            return MISSING_ASSET;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        Long recipient;
        try {
            recipient = Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            return INCORRECT_RECIPIENT;
        }

        Long asset;
        try {
            asset = Convert.parseUnsignedLong(assetValue);
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue);
            if (quantity <= 0 || quantity >= Nxt.MAX_ASSET_QUANTITY) {
                return INCORRECT_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
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

        Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
        if (assetBalance == null || quantity > assetBalance) {
            return NOT_ENOUGH_FUNDS;
        }

        int timestamp = Convert.getEpochTime();

        Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
        Transaction transaction = Nxt.getTransactionProcessor().newTransaction(timestamp, deadline, publicKey,
                recipient, 0, fee, referencedTransaction, attachment);
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
