package nxt.http;

import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;


public final class StartForging extends HttpRequestDispatcher.HttpRequestHandler {

    static final StartForging instance = new StartForging();

    private StartForging() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        Generator generator = Generator.startForging(secretPhrase);

        JSONObject response = new JSONObject();
        response.put("deadline", generator.getDeadline());
        return response;

    }

}
