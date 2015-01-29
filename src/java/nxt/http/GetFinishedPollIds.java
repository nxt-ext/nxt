package nxt.http;


import nxt.Nxt;
import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


//todo: is this call needed?
public class GetFinishedPollIds extends APIServlet.APIRequestHandler {
    static final GetFinishedPollIds instance = new GetFinishedPollIds();

    private GetFinishedPollIds() {
        super(new APITag[] {APITag.AE});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        int height = Nxt.getBlockchain().getHeight();

        JSONArray pollIds = new JSONArray();
        for (Poll poll : Poll.getPollsFinishingAtOrBefore(height)) {
            pollIds.add(Convert.toUnsignedLong(poll.getId()));
        }

        JSONObject response = new JSONObject();
        response.put("pollIds", pollIds);
        return response;

    }
}
