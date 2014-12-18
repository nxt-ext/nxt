package nxt.http;


import nxt.NxtException;
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
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Poll poll = ParameterParser.getPoll(req);
        if (!poll.isFinished()) {
            return UNKNOWN_POLL_RESULTS;
        }
        return JSONData.pollResults(poll);
    }
}
