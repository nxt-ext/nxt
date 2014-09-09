package nxt.http;

import nxt.Account;
import nxt.Currency;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetCurrenciesByIssuer extends APIServlet.APIRequestHandler {

    static final GetCurrenciesByIssuer instance = new GetCurrenciesByIssuer();

    private GetCurrenciesByIssuer() {
        super(new APITag[] {APITag.MS, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        List<Account> accounts = ParameterParser.getAccounts(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        response.put("currencies", accountsJSONArray);
        for (Account account : accounts) {
            JSONArray currenciesJSONArray = new JSONArray();
            for (Currency currency : Currency.getCurrencyIssuedBy(account.getId(), firstIndex, lastIndex)) {
                currenciesJSONArray.add(JSONData.currency(currency));
            }
            accountsJSONArray.add(currenciesJSONArray);
        }
        return response;
    }

}
