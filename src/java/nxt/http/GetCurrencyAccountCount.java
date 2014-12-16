package nxt.http;

import nxt.Account;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyAccountCount extends APIServlet.APIRequestHandler {

    static final GetCurrencyAccountCount instance = new GetCurrencyAccountCount();

    private GetCurrencyAccountCount() {
        super(new APITag[] {APITag.MS}, "currency", "height");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Currency currency = ParameterParser.getCurrency(req);
        int height = ParameterParser.getHeight(req);

        JSONObject response = new JSONObject();
        response.put("numberOfAccounts", Account.getCurrencyAccountCount(currency.getId(), height));
        return response;

    }

}
