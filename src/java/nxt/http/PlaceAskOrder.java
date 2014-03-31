package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.DUPLICATE_PRICE;
import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.INCORRECT_PRICE;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.MISSING_PRICE;
import static nxt.http.JSONResponses.MISSING_QUANTITY;
import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {
        super("asset", "quantity", "priceNXT", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");
        String priceValueNXT = Convert.emptyToNull(req.getParameter("priceNXT"));
        String priceValueNQT = Convert.emptyToNull(req.getParameter("priceNQT"));

        if (assetValue == null) {
            return MISSING_ASSET;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        } else if (priceValueNXT == null && priceValueNQT == null) {
            return MISSING_PRICE;
        } else if (priceValueNXT != null && priceValueNQT != null) {
            return DUPLICATE_PRICE;
        }

        long priceNQT;
        try {
            priceNQT = priceValueNQT != null ? Long.parseLong(priceValueNQT) : Convert.parseNXT(priceValueNXT);
            if (priceNQT <= 0 || priceNQT > Constants.MAX_BALANCE_NXT * Constants.ONE_NXT) {
                return INCORRECT_PRICE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_PRICE;
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
            if (quantity <= 0 || quantity > Constants.MAX_ASSET_QUANTITY) {
                return INCORRECT_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
        if (assetBalance == null || quantity > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, priceNQT);
        return createTransaction(req, account, attachment);

    }

}
