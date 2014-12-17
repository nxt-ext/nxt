package nxt.http;

import nxt.Account;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class CanDeleteCurrency extends APIServlet.APIRequestHandler {

    static final CanDeleteCurrency instance = new CanDeleteCurrency();

    private CanDeleteCurrency() {
        super(new APITag[] {APITag.MS}, "account", "currency");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        Account account = ParameterParser.getAccount(req);
        JSONObject response = new JSONObject();
        response.put("canDelete", currency.canBeDeletedBy(account.getId()));
        return response;
    }

}
