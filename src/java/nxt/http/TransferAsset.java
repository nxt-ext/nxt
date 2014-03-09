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

public final class TransferAsset extends CreateTransaction {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String recipientValue = req.getParameter("recipient");
        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");

        if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (assetValue == null) {
            return MISSING_ASSET;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
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

        Account account = getAccount(req);
        if (account == null) {
            return NOT_ENOUGH_FUNDS;
        }

        Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
        if (assetBalance == null || quantity > assetBalance) {
            return NOT_ENOUGH_FUNDS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
