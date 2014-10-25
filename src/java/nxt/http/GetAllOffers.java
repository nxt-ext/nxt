package nxt.http;

import nxt.CurrencyBuyOffer;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllOffers extends APIServlet.APIRequestHandler {

    static final GetAllOffers instance = new GetAllOffers();

    private GetAllOffers() {
        super(new APITag[] {APITag.MS}, "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray offerData = new JSONArray();

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        try (DbIterator<CurrencyBuyOffer> offers = CurrencyBuyOffer.getAll(firstIndex, lastIndex)) {
            for (CurrencyBuyOffer buyOffer : offers) {
                offerData.add(JSONData.offer(buyOffer));
            }
        }
        response.put("openOffers", offerData);
        return response;
    }

}
