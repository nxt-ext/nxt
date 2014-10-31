package nxt.http;


import nxt.Poll;
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

        String pollIdValue = Convert.emptyToNull(req.getParameter("poll"));
        if (pollIdValue == null) {
            return MISSING_POLL;
        }

        long pollId = Convert.parseUnsignedLong(pollIdValue);

        Poll poll = Poll.getPoll(pollId);
        if (poll == null) {
            return INCORRECT_POLL;
        }
        if (!poll.isFinished()) {
            return UNKNOWN_POLL_RESULTS;
        }

        return JSONData.pollResults(poll);
    }
}
