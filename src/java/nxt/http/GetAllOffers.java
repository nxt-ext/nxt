package nxt.http;

import nxt.CurrencyBuyOffer;
import nxt.CurrencyOffer;
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

        for (CurrencyOffer buyOffer : CurrencyBuyOffer.getAll(firstIndex, lastIndex)) {
            offerData.add(JSONData.offer(buyOffer));
        }

        response.put("openOffers", offerData);
        return response;
    }

}
