package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static nxt.http.JSONResponses.ASSET_NAME_ALREADY_USED;
import static nxt.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_ASSET_NAME;
import static nxt.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_ASSET_QUANTITY;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.MISSING_NAME;
import static nxt.http.JSONResponses.MISSING_QUANTITY;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class IssueAsset extends CreateTransaction {

    static final IssueAsset instance = new IssueAsset();

    private IssueAsset() {}

    private static final List<String> parameters = addCommonParameters(Arrays.asList(
            "name", "description", "quantity"));

    @Override
    List<String> getParameters() {
        return parameters;
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String quantityValue = req.getParameter("quantity");

        if (name == null) {
            return MISSING_NAME;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        }

        name = name.trim();
        if (name.length() < 3 || name.length() > 10) {
            return INCORRECT_ASSET_NAME_LENGTH;
        }

        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
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
            if (quantity <= 0 || quantity > Constants.MAX_ASSET_QUANTITY) {
                return INCORRECT_ASSET_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantity);
        return createTransaction(req, account, attachment);

    }

    @Override
    final int minimumFee() {
        return Constants.ASSET_ISSUANCE_FEE;
    }

}
