package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {
        super("asset", "quantityQNT", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);

        if (quantityQNT > account.getUnconfirmedAssetBalanceQNT(asset.getId())) {
            return NOT_ENOUGH_ASSETS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset.getId(), quantityQNT, priceNQT);
        return createTransaction(req, account, attachment);

    }

}
