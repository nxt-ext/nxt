package nxt.http;

import nxt.Account;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchAccounts extends APIServlet.APIRequestHandler {

    static final SearchAccounts instance = new SearchAccounts();

    private SearchAccounts() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        try (DbIterator<Account> accounts = Account.searchAccounts(query, firstIndex, lastIndex)) {
            for (Account account : accounts) {
                JSONObject accountJSON = new JSONObject();
                JSONData.putAccount(accountJSON, "account", account.getId());
                if (account.getName() != null) {
                    accountJSON.put("name", account.getName());
                }
                if (account.getDescription() != null) {
                    accountJSON.put("description", account.getDescription());
                }
                accountsJSONArray.add(accountJSON);
            }
        }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
