package nxt.http;

import nxt.Account;
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

import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_PRICE;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_PRICE;
import static nxt.http.JSONResponses.MISSING_QUANTITY;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class PlaceBidOrder extends HttpRequestHandler {

    static final PlaceBidOrder instance = new PlaceBidOrder();

    private PlaceBidOrder() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String secretPhrase = req.getParameter("secretPhrase");
        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");
        String priceValue = req.getParameter("price");
        String feeValue = req.getParameter("fee");
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (assetValue == null) {
            return MISSING_ASSET;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        } else if (priceValue == null) {
            return MISSING_PRICE;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        long price;
        try {
            price = Long.parseLong(priceValue);
            if (price <= 0 || price > Nxt.MAX_BALANCE * 100L) {
                return INCORRECT_PRICE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_PRICE;
        }

        Long asset;
        try {
            asset = Convert.parseUnsignedLong(assetValue);
        } catch (NumberFormatException e) {
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
        if (account == null || quantity * price + fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        int timestamp = Convert.getEpochTime();

        Attachment attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
        Transaction transaction = Transaction.newTransaction(timestamp, deadline, publicKey,
                Genesis.CREATOR_ID, 0, fee, referencedTransaction, attachment);
        transaction.sign(secretPhrase);

        Blockchain.broadcast(transaction);

        JSONObject response = new JSONObject();
        response.put("transaction", transaction.getStringId());
        return response;
    }

}
