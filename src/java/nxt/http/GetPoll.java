package nxt.http;

import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static nxt.http.JSONResponses.INCORRECT_POLL;
import static nxt.http.JSONResponses.MISSING_POLL;
import static nxt.http.JSONResponses.UNKNOWN_POLL;

public final class GetPoll extends APIServlet.APIRequestHandler {

    static final GetPoll instance = new GetPoll();

    private GetPoll() {
        super("poll");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String poll = req.getParameter("poll");
        if (poll == null) {
            return MISSING_POLL;
        }

        Poll pollData;
        try {
            pollData = Poll.getPoll(Convert.parseUnsignedLong(poll));
            if (pollData == null) {
                return UNKNOWN_POLL;
            }
        } catch (RuntimeException e) {
            return INCORRECT_POLL;
        }

        JSONObject response = new JSONObject();
        if (pollData.getName().length() > 0) {
            response.put("name", pollData.getName());
        }
        if (pollData.getDescription().length() > 0) {
            response.put("description", pollData.getDescription());
        }
        JSONArray options = new JSONArray();
        Collections.addAll(options, pollData.getOptions());
        response.put("options", options);
        response.put("minNumberOfOptions", pollData.getMinNumberOfOptions());
        response.put("maxNumberOfOptions", pollData.getMaxNumberOfOptions());
        response.put("optionsAreBinary", pollData.isOptionsAreBinary());
        JSONArray voters = new JSONArray();
        for (Long voterId : pollData.getVoters().keySet()) {
            voters.add(Convert.toUnsignedLong(voterId));
        }
        response.put("voters", voters);

        return response;
    }

}
