package nxt.http;

import nxt.Account;
import nxt.Currency;
import nxt.Exchange;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetExchanges extends APIServlet.APIRequestHandler {

    static final GetExchanges instance = new GetExchanges();

    private GetExchanges() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex", "includeCurrencyInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String currencyId = Convert.emptyToNull(req.getParameter("currency"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));
        boolean includeCurrencyInfo = !"false".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray exchangesData = new JSONArray();
        DbIterator<Exchange> exchanges = null;
        try {
            if (accountId == null) {
                Currency currency = ParameterParser.getCurrency(req);
                exchanges = currency.getExchanges(firstIndex, lastIndex);
            } else if (currencyId == null) {
                Account account = ParameterParser.getAccount(req);
                exchanges = account.getExchanges(firstIndex, lastIndex);
            } else {
                Currency currency = ParameterParser.getCurrency(req);
                Account account = ParameterParser.getAccount(req);
                exchanges = Exchange.getAccountCurrencyExchanges(account.getId(), currency.getId(), firstIndex, lastIndex);
            }
            while (exchanges.hasNext()) {
                exchangesData.add(JSONData.exchange(exchanges.next(), includeCurrencyInfo));
            }
        } finally {
            DbUtils.close(exchanges);
        }
        response.put("exchanges", exchangesData);

        return response;
    }

    @Override
    boolean startDbTransaction() {
        return true;
    }

}
