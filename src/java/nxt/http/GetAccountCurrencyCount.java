package nxt.http;

import nxt.Account;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountCurrencyCount extends APIServlet.APIRequestHandler {

    static final GetAccountCurrencyCount instance = new GetAccountCurrencyCount();

    private GetAccountCurrencyCount() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MS}, "account", "height");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        int height = ParameterParser.getHeight(req);

        JSONObject response = new JSONObject();
        response.put("numberOfCurrencies", Account.getAccountCurrencyCount(account.getId(), height));
        return response;
    }

}
