package nxt.http;


import nxt.PollResults;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.*;

public class GetPollResults extends APIServlet.APIRequestHandler {
    static final GetPollResults instance = new GetPollResults();

    private GetPollResults() {
        super(new APITag[] {APITag.VS}, "poll");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String pollId = req.getParameter("poll");
        if (pollId == null) {
            return MISSING_POLL;
        }

        PollResults pollResults;
        try {
            pollResults = PollResults.get(Convert.parseUnsignedLong(pollId));

            if (pollResults == null) {
                return UNKNOWN_POLL_RESULTS;
            }
        } catch (RuntimeException e) {
            return INCORRECT_POLL;
        }

        return JSONData.pollResults(pollResults);
    }
}
