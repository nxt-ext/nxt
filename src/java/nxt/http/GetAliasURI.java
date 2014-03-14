package nxt.http;

import nxt.Alias;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static nxt.http.JSONResponses.MISSING_ALIAS;
import static nxt.http.JSONResponses.UNKNOWN_ALIAS;

public final class GetAliasURI extends APIServlet.APIRequestHandler {

    static final GetAliasURI instance = new GetAliasURI();

    private GetAliasURI() {}

    private static final List<String> parameters = Arrays.asList("alias");

    @Override
    List<String> getParameters() {
        return parameters;
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String alias = req.getParameter("alias");
        if (alias == null) {
            return MISSING_ALIAS;
        }

        Alias aliasData = Alias.getAlias(alias.toLowerCase());
        if (aliasData == null) {
            return UNKNOWN_ALIAS;
        }

        if (aliasData.getURI().length() > 0) {

            JSONObject response = new JSONObject();
            response.put("uri", aliasData.getURI());
            return response;

        } else {
            return JSON.emptyJSON;
        }
    }

}
