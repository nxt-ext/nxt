package nxt.http;

import nxt.Exchange;
import nxt.Nxt;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.*;

public final class GetExchangesByExchangeRequest extends APIServlet.APIRequestHandler {

    static final GetExchangesByExchangeRequest instance = new GetExchangesByExchangeRequest();

    private GetExchangesByExchangeRequest() {
        super(new APITag[] {APITag.MS}, "transaction", "includeCurrencyInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }
        long transactionId = Convert.parseUnsignedLong(transactionIdString);
        boolean includeCurrencyInfo = !"false".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));
        DbIterator<Exchange> exchanges = Exchange.getExchanges(transactionId);
        JSONObject response = new JSONObject();
        JSONArray exchangesData = new JSONArray();
        while (exchanges.hasNext()) {
            exchangesData.add(JSONData.exchange(exchanges.next(), includeCurrencyInfo));
        }
        response.put("exchanges", exchangesData);
        return response;
    }

}
