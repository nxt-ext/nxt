package nxt.http;

import nxt.Exchange;
import nxt.NxtException;
import nxt.db.FilteringIterator;
import nxt.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllExchanges extends APIServlet.APIRequestHandler {

    static final GetAllExchanges instance = new GetAllExchanges();

    private GetAllExchanges() {
        super(new APITag[] {APITag.MS}, "timestamp", "firstIndex", "lastIndex", "includeCurrencyInfo");
    }
    
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCurrencyInfo = !"false".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        JSONObject response = new JSONObject();
        JSONArray exchanges = new JSONArray();
        try (FilteringIterator<Exchange> exchangeIterator = new FilteringIterator<>(Exchange.getAllExchanges(0, -1),
                new Filter<Exchange>() {
                    @Override
                    public boolean ok(Exchange exchange) {
                        return exchange.getTimestamp() >= timestamp;
                    }
                }, firstIndex, lastIndex)) {
            while (exchangeIterator.hasNext()) {
                exchanges.add(JSONData.exchange(exchangeIterator.next(), includeCurrencyInfo));
            }
        }
        response.put("exchanges", exchanges);
        return response;
    }

}
