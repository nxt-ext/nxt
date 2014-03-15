package nxt.http;

import nxt.Account;
import nxt.Token;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_WEBSITE;
import static nxt.http.JSONResponses.MISSING_TOKEN;
import static nxt.http.JSONResponses.MISSING_WEBSITE;

public final class DecodeToken extends APIServlet.APIRequestHandler {

    static final DecodeToken instance = new DecodeToken();

    private DecodeToken() {
        super("website", "token");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String website = req.getParameter("website");
        String tokenString = req.getParameter("token");
        if (website == null) {
            return MISSING_WEBSITE;
        } else if (tokenString == null) {
            return MISSING_TOKEN;
        }

        try {

            Token token = Token.parseToken(tokenString, website.trim());

            JSONObject response = new JSONObject();
            response.put("account", Convert.toUnsignedLong(Account.getId(token.getPublicKey())));
            response.put("timestamp", token.getTimestamp());
            response.put("valid", token.isValid());

            return response;

        } catch (RuntimeException e) {
            return INCORRECT_WEBSITE;
        }
    }

}
