package nxt.http;

import nxt.CurrencyExchange;
import nxt.NxtException;
import nxt.Trade;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyExchanges extends APIServlet.APIRequestHandler {

    static final GetCurrencyExchanges instance = new GetCurrencyExchanges();

    private GetCurrencyExchanges() {
        super(new APITag[] {APITag.MS}, "currency", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long currencyId = ParameterParser.getCurrency(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();

        JSONArray exchangesData = new JSONArray();
//        for (CurrencyExchange exchange : CurrencyExchange.getExchanges(currencyId, firstIndex, lastIndex)) {
//            exchangesData.add(JSONData.exchange(exchange));
//        }
        response.put("trades", exchangesData);
        return response;
    }

}
