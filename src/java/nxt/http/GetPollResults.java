package nxt.http;


import nxt.NxtException;
import nxt.Poll;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.POLL_RESULTS_NOT_AVAILABLE;

public class GetPollResults extends APIServlet.APIRequestHandler {
    static final GetPollResults instance = new GetPollResults();

    private GetPollResults() {
        super(new APITag[]{APITag.VS}, "poll");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Poll poll = ParameterParser.getPoll(req);
        List<Poll.PollResult> pollResults = poll.getResults();
        if (pollResults == null) {
            return POLL_RESULTS_NOT_AVAILABLE;
        }
        return JSONData.pollResults(poll, poll.getResults());
    }
}
