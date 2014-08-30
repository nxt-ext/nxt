package nxt.http;

import nxt.NxtException;
import nxt.Trade;
import nxt.Transaction;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

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

        //TODO: optimize to do the filtering by timestamp in the database
        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        try (DbIterator<Trade> trades = Trade.getAllTrades(0, -1)) {
            while (trades.hasNext()) {
                Trade trade = trades.next();
                if (trade.getTimestamp() >= timestamp) {
                    tradesData.add(JSONData.trade(trade));
                }
            }
        }
        response.put("trades", tradesData);
        return response;
    }

}
