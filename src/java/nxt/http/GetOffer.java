package nxt.http;

import nxt.CurrencyBuyOffer;
import nxt.CurrencySellOffer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetOffer extends APIServlet.APIRequestHandler {

    static final GetOffer instance = new GetOffer();

    private GetOffer() {
        super(new APITag[] {APITag.MS}, "offer");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        JSONObject response = new JSONObject();
        long offerId = ParameterParser.getLong(req, "offer", Long.MIN_VALUE, Long.MAX_VALUE, true);
        CurrencyBuyOffer buyOffer = CurrencyBuyOffer.getOffer(offerId);
        CurrencySellOffer sellOffer = CurrencySellOffer.getOffer(offerId);
        response.put("buyOffer", JSONData.offer(buyOffer));
        response.put("sellOffer", JSONData.offer(sellOffer));
        return response;
    }

}
