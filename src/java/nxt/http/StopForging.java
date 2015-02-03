package nxt.http;

import nxt.Generator;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public final class StopForging extends APIServlet.APIRequestHandler {

    static final StopForging instance = new StopForging();

    private StopForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase", "adminPassword");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        JSONObject response = new JSONObject();
        if (secretPhrase != null) {
            Generator generator = Generator.stopForging(secretPhrase);
            response.put("foundAndStopped", generator != null);
        } else {
            API.verifyPassword(req);
            int count = Generator.stopForging();
            response.put("stopped", count);
        }
        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
