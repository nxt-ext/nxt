package nxt.http;


import nxt.Poll;
import nxt.Vote;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static nxt.http.JSONResponses.INCORRECT_POLL;
import static nxt.http.JSONResponses.MISSING_POLL;


public class GetPollVotes extends APIServlet.APIRequestHandler  {
    static final GetPollVotes instance = new GetPollVotes();

    private GetPollVotes() { super(new APITag[] {APITag.VS}, "poll"); }


    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        String pollIdValue = Convert.emptyToNull(req.getParameter("poll"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        if (pollIdValue == null) {
            return MISSING_POLL;
        }

        long pollId = Convert.parseUnsignedLong(pollIdValue);

        Poll poll = Poll.getPoll(pollId);
        if (poll == null) {
            return INCORRECT_POLL;
        }

        List<Vote> votes = Vote.getVotes(pollId, firstIndex, lastIndex).toList();

        JSONArray votesJson = new JSONArray();
        for (Vote vote : votes) {
            votesJson.add(JSONData.vote(poll, vote));
        }

        JSONObject response = new JSONObject();
        response.put("votes", votesJson);
        return response;
    }
}
