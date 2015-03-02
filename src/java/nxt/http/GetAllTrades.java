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
        super(new APITag[] {APITag.AE}, "timestamp", "firstIndex", "lastIndex", "includeAssetInfo");
    }
    
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = !"false".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray trades = new JSONArray();
        try (DbIterator<Trade> tradeIterator = Trade.getAllTrades(firstIndex, lastIndex)) {
            while (tradeIterator.hasNext()) {
                Trade trade = tradeIterator.next();
                if (trade.getTimestamp() < timestamp) {
                    break;
                }
                trades.add(JSONData.trade(trade, includeAssetInfo));
            }
        }
        response.put("trades", trades);
        return response;
    }

}
