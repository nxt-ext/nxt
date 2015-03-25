package nxt.http;

import nxt.NxtException;
import nxt.Trade;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetOrderTrades extends APIServlet.APIRequestHandler {

    static final GetOrderTrades instance = new GetOrderTrades();

    private GetOrderTrades() {
        super(new APITag[] {APITag.AE}, "askOrder", "bidOrder", "includeAssetInfo", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long askOrderId = ParameterParser.getUnsignedLong(req, "askOrder", false);
        long bidOrderId = ParameterParser.getUnsignedLong(req, "bidOrder", false);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        if (askOrderId == 0 && bidOrderId == 0) {
            return JSONResponses.missing("askOrder", "bidOrder");
        }

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        if (askOrderId != 0 && bidOrderId != 0) {
            Trade trade = Trade.getTrade(askOrderId, bidOrderId);
            if (trade != null) {
                tradesData.add(JSONData.trade(trade, false));
            }
        } else {
            DbIterator<Trade> trades = null;
            try {
                if (askOrderId != 0) {
                    trades = Trade.getAskOrderTrades(askOrderId, firstIndex, lastIndex);
                } else {
                    trades = Trade.getBidOrderTrades(bidOrderId, firstIndex, lastIndex);
                }
                while (trades.hasNext()) {
                    Trade trade = trades.next();
                    tradesData.add(JSONData.trade(trade, includeAssetInfo));
                }
            } finally {
                DbUtils.close(trades);
            }
        }
        response.put("trades", tradesData);

        return response;
    }

}
