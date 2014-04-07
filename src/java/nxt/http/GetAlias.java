package nxt.http;

import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS;
import static nxt.http.JSONResponses.MISSING_ALIAS;
import static nxt.http.JSONResponses.UNKNOWN_ALIAS;

public final class GetAlias extends APIServlet.APIRequestHandler {

    static final GetAlias instance = new GetAlias();

    private GetAlias() {
        super("alias");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String alias = req.getParameter("alias");
        if (alias == null) {
            return MISSING_ALIAS;
        }

        Alias aliasData;
        try {
            aliasData = Alias.getAlias(Convert.parseUnsignedLong(alias));
            if (aliasData == null) {
                return UNKNOWN_ALIAS;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ALIAS;
        }

        return JSONData.alias(aliasData);
    }

}
