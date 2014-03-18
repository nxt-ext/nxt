package nxt.http;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nxt.Asset;
import nxt.Nxt;
import nxt.Trade;
import static nxt.http.JSONResponses.INCORRECT_TIMESTAMP;
import static nxt.http.JSONResponses.MISSING_TIMESTAMP;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAllTrades extends APIServlet.APIRequestHandler {

    static final GetAllTrades instance = new GetAllTrades();

    private GetAllTrades() {
        super("timestamp");
    }
    
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String timestampValue = req.getParameter("timestamp");
        if (timestampValue == null) {
            return MISSING_TIMESTAMP;
        }

        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
            if (timestamp < 0) {
                return INCORRECT_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_TIMESTAMP;
        }

        JSONObject response = new JSONObject();
        
        JSONArray tradesData = new JSONArray();

        try {
            Collection<List<Trade>> trades = Trade.getAllTrades();

            for (List<Trade> assetTrades : trades) {
                for (Trade trade : assetTrades) {
                    if (trade.getTimeStamp() >= timestamp) {
                        JSONObject tradeData = new JSONObject();
                        tradeData.put("timestamp", trade.getTimeStamp());
                        tradeData.put("quantity", trade.getQuantity());
                        tradeData.put("price", trade.getPrice());
                        tradeData.put("assetId", Convert.toUnsignedLong(trade.getAssetId()));
                        tradesData.add(tradeData);
                    }
                }
            }
        } catch (RuntimeException e) {
            response.put("error", e.toString());
        }

        response.put("trades", tradesData);
        return response;
    }

}
