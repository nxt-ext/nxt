package nxt.http;

import nxt.Currency;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllCurrencies extends APIServlet.APIRequestHandler {

    static final GetAllCurrencies instance = new GetAllCurrencies();

    private GetAllCurrencies() {
        super(new APITag[] {APITag.MS}, "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray currenciesJSONArray = new JSONArray();
        response.put("currencies", currenciesJSONArray);
        for (Currency currency : Currency.getAllCurrencies(firstIndex, lastIndex)) {
            currenciesJSONArray.add(JSONData.currency(currency));
        }
        return response;
    }

}
