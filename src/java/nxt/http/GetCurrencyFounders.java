package nxt.http;

import nxt.Currency;
import nxt.CurrencyFounder;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyFounders extends APIServlet.APIRequestHandler {

    static final GetCurrencyFounders instance = new GetCurrencyFounders();

    private GetCurrencyFounders() {
        super(new APITag[] {APITag.MS}, "currency");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Currency currencyId = ParameterParser.getCurrency(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray foundersJSONArray = new JSONArray();
        response.put("founders", foundersJSONArray);

        try (DbIterator<CurrencyFounder> currencyFounders = CurrencyFounder.getCurrencyFounders(currencyId.getId(), firstIndex, lastIndex)) {
            for (CurrencyFounder founder : currencyFounders) {
                foundersJSONArray.add(JSONData.currencyFounder(founder));
            }
        }
        return response;
    }

}
