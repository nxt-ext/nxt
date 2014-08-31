package nxt.http;

import nxt.Alias;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliases extends APIServlet.APIRequestHandler {

    static final GetAliases instance = new GetAliases();

    private GetAliases() {
        super(new APITag[] {APITag.ALIASES}, "timestamp", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        int timestamp = ParameterParser.getTimestamp(req);
        Long accountId = ParameterParser.getAccount(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray aliases = new JSONArray();
        try (DbIterator<Alias> aliasIterator = Alias.getAliasesByOwner(accountId, 0, -1)) {
            int count = 0;
            while (aliasIterator.hasNext() && count <= lastIndex) {
                Alias alias = aliasIterator.next();
                if (alias.getTimestamp() >= timestamp) {
                    if (count >= firstIndex) {
                        aliases.add(JSONData.alias(alias));
                    }
                    count += 1;
                }
            }
        }

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;
    }

}
