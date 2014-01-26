package nxt.http;

import nxt.Alias;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAliasURI extends HttpRequestHandler {

    static final GetAliasURI instance = new GetAliasURI();

    private GetAliasURI() {}

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

                if (aliasData.uri.length() > 0) {

                    response.put("uri", aliasData.uri);

                }

            }

        }
        return response;
    }

}
