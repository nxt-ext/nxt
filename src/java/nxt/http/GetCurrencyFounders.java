package nxt.http;

import nxt.Account;
import nxt.Currency;
import nxt.CurrencyFounder;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyFounders extends APIServlet.APIRequestHandler {

    static final GetCurrencyFounders instance = new GetCurrencyFounders();

    private GetCurrencyFounders() {
        super(new APITag[] {APITag.MS}, "currency", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String currencyId = Convert.emptyToNull(req.getParameter("currency"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));
        if (currencyId == null && accountId == null) {
            return JSONResponses.MISSING_CURRENCY_ACCOUNT;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray foundersJSONArray = new JSONArray();
        response.put("founders", foundersJSONArray);

        DbIterator<CurrencyFounder> founders = null;
        if (accountId == null) {
            Currency currency = ParameterParser.getCurrency(req);
            founders = CurrencyFounder.getCurrencyFounders(currency.getId(), firstIndex, lastIndex);
        } else if (currencyId == null) {
            Account account = ParameterParser.getAccount(req);
            founders = CurrencyFounder.getFoundersCurrency(account.getId(), firstIndex, lastIndex);
        }
        if (founders != null) {
            for (CurrencyFounder founder : founders) {
                foundersJSONArray.add(JSONData.currencyFounder(founder));
            }
        }
        return response;
    }
}
