package nxt.http;

import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAlias extends HttpRequestHandler {

    static final GetAlias instance = new GetAlias();

    private GetAlias() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String alias = req.getParameter("alias");
        if (alias == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"alias\" not specified");

        } else {

            try {

                Alias aliasData = Alias.getAlias(Convert.parseUnsignedLong(alias));
                if (aliasData == null) {

                    response.put("errorCode", 5);
                    response.put("errorDescription", "Unknown alias");

                } else {

                    response.put("account", Convert.convert(aliasData.account.id));
                    response.put("alias", aliasData.alias);
                    if (aliasData.uri.length() > 0) {

                        response.put("uri", aliasData.uri);

                    }
                    response.put("timestamp", aliasData.timestamp);

                }

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"alias\"");

            }

        }

        return response;
    }

}
