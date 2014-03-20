package nxt.http;

import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TIMESTAMP;
import static nxt.http.JSONResponses.MISSING_TIMESTAMP;

public final class GetAliasIds extends APIServlet.APIRequestHandler {

    static final GetAliasIds instance = new GetAliasIds();

    private GetAliasIds() {
        super("timestamp");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String timestampValue = req.getParameter("timestamp");
        if (timestampValue == null) {
            return MISSING_TIMESTAMP;
        }

        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
            if (timestamp < 0) {
                return INCORRECT_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_TIMESTAMP;
        }

        JSONArray aliasIds = new JSONArray();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getTimestamp() >= timestamp) {
                aliasIds.add(Convert.toUnsignedLong(alias.getId()));
            }
        }

        JSONObject response = new JSONObject();

        response.put("aliasIds", aliasIds);
        return response;
    }

}
