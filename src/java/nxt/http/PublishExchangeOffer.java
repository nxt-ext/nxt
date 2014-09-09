package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class PublishExchangeOffer extends CreateTransaction {

    static final PublishExchangeOffer instance = new PublishExchangeOffer();

    private PublishExchangeOffer() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "buyRateNQT", "sellRateNQT",
                "totalBuyLimit", "totalSellLimit", "initialBuySupply", "initialSellSupply", "expirationHeight");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        long buyRateNQT = ParameterParser.getLong(req, "buyRateNQT", 0, Long.MAX_VALUE, true);
        long sellRateNQT= ParameterParser.getLong(req, "sellRateNQT", 0, Long.MAX_VALUE, true);
        long totalBuyLimit = ParameterParser.getLong(req, "totalBuyLimit", 0, Long.MAX_VALUE, true);
        long totalSellLimit = ParameterParser.getLong(req, "totalSellLimit", 0, Long.MAX_VALUE, true);
        long initialBuySupply = ParameterParser.getLong(req, "initialBuySupply", 0, Long.MAX_VALUE, true);
        long initialSellSupply = ParameterParser.getLong(req, "initialSellSupply", 0, Long.MAX_VALUE, true);
        int expirationHeight = ParameterParser.getInt(req, "expirationHeight", 0, Integer.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        // TODO understand what to check
//        if (quantityQNT > account.getUnconfirmedCurrencyBalanceQNT(currency.getId())) {
//            return NOT_ENOUGH_CURRENCY;
//        }

        Attachment attachment = new Attachment.MonetarySystemExchangeOfferPublication(currency.getId(), buyRateNQT, sellRateNQT,
                totalBuyLimit, totalSellLimit, initialBuySupply, initialSellSupply, expirationHeight);
        return createTransaction(req, account, attachment);

    }

}
