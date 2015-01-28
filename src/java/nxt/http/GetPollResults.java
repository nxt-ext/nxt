package nxt.http;


import nxt.Constants;
import nxt.NxtException;
import nxt.Poll;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.UNKNOWN_POLL_RESULTS;

public class GetPollResults extends APIServlet.APIRequestHandler {
    static final GetPollResults instance = new GetPollResults();

    private GetPollResults() {
        super(new APITag[]{APITag.VS}, "poll");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Poll poll = ParameterParser.getPoll(req);
        List<Poll.PartialPollResult> results;

        if (Constants.isPollsProcessing) {
            if (!poll.isFinished()) {
                return UNKNOWN_POLL_RESULTS;
            }
            results = Poll.getResults(poll.getId());
        } else {
            results = poll.countResults();
        }
        return JSONData.pollResults(poll, results);
    }
}
