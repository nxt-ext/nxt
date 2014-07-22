package nxt.http;

import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetActivePollIds extends APIServlet.APIRequestHandler {

    static final GetActivePollIds instance = new GetActivePollIds();

    private GetActivePollIds() {
        super(new APITag[] {APITag.VS});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray pollIds = new JSONArray();
        for (Poll poll : Poll.getActivePolls()) {
            pollIds.add(Convert.toUnsignedLong(poll.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("pollIds", pollIds);
        return response;

    }
}
