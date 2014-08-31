package nxt.http;

import nxt.NxtException;
import nxt.Trade;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllTrades extends APIServlet.APIRequestHandler {

    static final GetAllTrades instance = new GetAllTrades();

    private GetAllTrades() {
        super(new APITag[] {APITag.AE}, "timestamp", "firstIndex", "lastIndex");
    }
    
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        try (DbIterator<Trade> trades = Trade.getAllTrades(0, -1)) {
            int count = 0;
            while (trades.hasNext() && count <= lastIndex) {
                Trade trade = trades.next();
                if (trade.getTimestamp() >= timestamp) {
                    if (count >= firstIndex) {
                        tradesData.add(JSONData.trade(trade));
                    }
                    count += 1;
                }
            }
        }
        response.put("trades", tradesData);
        return response;
    }

}
