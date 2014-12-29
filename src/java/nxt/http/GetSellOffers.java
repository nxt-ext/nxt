package nxt.http;

import nxt.Account;
import nxt.Currency;
import nxt.CurrencySellOffer;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetSellOffers extends APIServlet.APIRequestHandler {

    static final GetSellOffers instance = new GetSellOffers();

    private GetSellOffers() {
        super(new APITag[] {APITag.MS}, "currency", "account", "availableOnly", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String currencyId = Convert.emptyToNull(req.getParameter("currency"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));
        boolean availableOnly = "true".equalsIgnoreCase(req.getParameter("availableOnly"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray offerData = new JSONArray();
        response.put("offers", offerData);

        DbIterator<CurrencySellOffer> offers= null;
        try {
            if (accountId == null) {
                Currency currency = ParameterParser.getCurrency(req);
                offers = CurrencySellOffer.getOffers(currency, availableOnly, firstIndex, lastIndex);
            } else if (currencyId == null) {
                Account account = ParameterParser.getAccount(req);
                offers = CurrencySellOffer.getOffers(account, availableOnly, firstIndex, lastIndex);
            } else {
                Currency currency = ParameterParser.getCurrency(req);
                Account account = ParameterParser.getAccount(req);
                CurrencySellOffer offer = CurrencySellOffer.getOffer(currency, account);
                if (offer != null) {
                    offerData.add(JSONData.offer(offer));
                }
                return response;
            }
            while (offers.hasNext()) {
                offerData.add(JSONData.offer(offers.next()));
            }
        } finally {
            DbUtils.close(offers);
        }

        return response;
    }

}
