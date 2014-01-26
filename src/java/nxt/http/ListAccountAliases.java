package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class ListAccountAliases extends HttpRequestHandler {

    static final ListAccountAliases instance = new ListAccountAliases();

    private ListAccountAliases() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String account = req.getParameter("account");
        if (account == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"account\" not specified");

        } else {

            try {

                Long accountId = Convert.parseUnsignedLong(account);
                Account accountData = Account.getAccount(accountId);
                if (accountData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown account");

                } else {

                    JSONArray aliases = new JSONArray();
                    for (Alias alias : Alias.allAliases) {

                        if (alias.account.id.equals(accountId)) {

                            JSONObject aliasData = new JSONObject();
                            aliasData.put("alias", alias.alias);
                            aliasData.put("uri", alias.uri);
                            aliases.add(aliasData);

                        }

                    }
                    response.put("aliases", aliases);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"account\"");

            }

        }

        return response;
    }

}
