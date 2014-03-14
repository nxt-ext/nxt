package nxt.http;

import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;


public final class StartForging extends APIServlet.APIRequestHandler {

    static final StartForging instance = new StartForging();

    private StartForging() {}

    private static final List<String> parameters = Arrays.asList("secretPhrase");

    @Override
    List<String> getParameters() {
        return parameters;
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        Generator generator = Generator.startForging(secretPhrase);
        if (generator == null) {
            return UNKNOWN_ACCOUNT;
        }

        JSONObject response = new JSONObject();
        response.put("deadline", generator.getDeadline());
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
