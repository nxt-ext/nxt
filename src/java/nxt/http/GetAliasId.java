package nxt.http;

import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAliasId extends HttpRequestHandler {

    static final GetAliasId instance = new GetAliasId();

    private GetAliasId() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String alias = req.getParameter("alias");
        if (alias == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"alias\" not specified");

        } else {

            Alias aliasData = Alias.getAlias(alias.toLowerCase());
            if (aliasData == null) {

                response.put("errorCode", 5);
                response.put("errorDescription", "Unknown alias");

            } else {

                response.put("id", Convert.convert(aliasData.id));

            }

        }
        return response;
    }

}
