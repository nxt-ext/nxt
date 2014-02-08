package nxt.http;

import nxt.Account;
import nxt.Asset;
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

import static nxt.http.JSONResponses.ASSET_NAME_ALREADY_USED;
import static nxt.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_ASSET_ISSUANCE_FEE;
import static nxt.http.JSONResponses.INCORRECT_ASSET_NAME;
import static nxt.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_ASSET_QUANTITY;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.MISSING_FEE;
import static nxt.http.JSONResponses.MISSING_NAME;
import static nxt.http.JSONResponses.MISSING_QUANTITY;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class IssueAsset extends HttpRequestHandler {

    static final IssueAsset instance = new IssueAsset();

    private IssueAsset() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String secretPhrase = req.getParameter("secretPhrase");
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String quantityValue = req.getParameter("quantity");
        String feeValue = req.getParameter("fee");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (name == null) {
            return MISSING_NAME;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        } else if (feeValue == null) {
            return MISSING_FEE;
        }

        name = name.trim();
        if (name.length() < 3 || name.length() > 10) {
            return INCORRECT_ASSET_NAME_LENGTH;
        }

        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Convert.alphabet.indexOf(normalizedName.charAt(i)) < 0) {
                return INCORRECT_ASSET_NAME;
            }
        }

        if (Asset.getAsset(normalizedName) != null) {
            return ASSET_NAME_ALREADY_USED;
        }
        if (description != null && description.length() > 1000) {
            return INCORRECT_ASSET_DESCRIPTION;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue);
            if (quantity <= 0 || quantity > Nxt.MAX_ASSET_QUANTITY) {
                return INCORRECT_ASSET_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
        }

        int fee;
        try {
            fee = Integer.parseInt(feeValue);
            if (fee < Nxt.ASSET_ISSUANCE_FEE) {
                return INCORRECT_ASSET_ISSUANCE_FEE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_FEE;
        }

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        Account account = Account.getAccount(publicKey);
        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        int timestamp = Convert.getEpochTime();
        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantity);
        Transaction transaction = Transaction.newTransaction(timestamp, (short)1440, publicKey,
                Genesis.CREATOR_ID, 0, fee, null, attachment);
        transaction.sign(secretPhrase);

        Blockchain.broadcast(transaction);

        JSONObject response = new JSONObject();
        response.put("transaction", transaction.getStringId());
        return response;

    }

}
