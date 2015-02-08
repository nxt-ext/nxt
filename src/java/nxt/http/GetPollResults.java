package nxt.http;


import nxt.Constants;
import nxt.NxtException;
import nxt.Poll;
import nxt.PollCounting;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.POLL_RESULTS_NOT_AVAILABLE;

public class GetPollResults extends APIServlet.APIRequestHandler {
    static final GetPollResults instance = new GetPollResults();

    private GetPollResults() {
        super(new APITag[]{APITag.VS}, "poll", "votingModel", "holding", "minBalance", "minBalanceModel");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Poll poll = ParameterParser.getPoll(req);
        List<Poll.PollResult> pollResults;
        if (Convert.emptyToNull(req.getParameter("votingModel")) == null) {
            pollResults = poll.getResults();
        } else {
            byte votingModel = ParameterParser.getByte(req, "votingModel", Constants.VOTING_MODEL_NQT, Constants.VOTING_MODEL_CURRENCY, true);
            long holdingId = ParameterParser.getLong(req, "holding", Long.MIN_VALUE, Long.MAX_VALUE, false);
            long minBalance = ParameterParser.getLong(req, "minBalance", 0, Long.MAX_VALUE, false);
            byte minBalanceModel = ParameterParser.getByte(req, "minBalanceModel", Constants.VOTING_MINBALANCE_NQT, Constants.VOTING_MINBALANCE_CURRENCY, false);
            PollCounting pollCounting = new PollCounting(votingModel, holdingId, minBalance, minBalanceModel);
            pollCounting.validate();
            pollResults = poll.getResults(pollCounting);
        }
        if (pollResults == null) {
            return POLL_RESULTS_NOT_AVAILABLE;
        }
        return JSONData.pollResults(poll, pollResults);
    }
}
