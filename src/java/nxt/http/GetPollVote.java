package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.Poll;
import nxt.Vote;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPollVote extends APIServlet.APIRequestHandler  {
    static final GetPollVote instance = new GetPollVote();

    private GetPollVote() {
        super(new APITag[] {APITag.VS}, "poll", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Poll poll = ParameterParser.getPoll(req);
        Account account = ParameterParser.getAccount(req);
        Vote vote = Vote.getVote(poll.getId(), account.getId());
        if (vote != null) {
            return JSONData.vote(vote);
        }
        return JSON.emptyJSON;
    }
}
