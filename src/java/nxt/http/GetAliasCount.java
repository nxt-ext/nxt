package nxt.http;

import nxt.Alias;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliasCount extends APIServlet.APIRequestHandler {

    static final GetAliasCount instance = new GetAliasCount();

    private GetAliasCount() {
        super(new APITag[] {APITag.ALIASES}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final long accountId = ParameterParser.getAccount(req).getId();
        JSONObject response = new JSONObject();
        response.put("numberOfAliases", Alias.getAccountAliasCount(accountId));
        return response;
    }

}
