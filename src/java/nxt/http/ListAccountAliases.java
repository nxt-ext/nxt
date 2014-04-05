package nxt.http;

import nxt.Alias;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ListAccountAliases extends APIServlet.APIRequestHandler {

    static final ListAccountAliases instance = new ListAccountAliases();

    private ListAccountAliases() {
        super("account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long accountId = ParameterParser.getAccount(req).getId();
        JSONArray aliases = new JSONArray();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getAccount().getId().equals(accountId)) {
                aliases.add(JSONData.alias(alias));
            }
        }

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);

        return response;
    }

}
