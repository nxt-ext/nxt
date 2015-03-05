package nxt.http;

import nxt.NxtException;
import nxt.Poll;
import nxt.Vote;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPollVotes extends APIServlet.APIRequestHandler  {
    static final GetPollVotes instance = new GetPollVotes();

    private GetPollVotes() { super(new APITag[] {APITag.VS}, "poll", "firstIndex", "lastIndex"); }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        Poll poll = ParameterParser.getPoll(req);

        JSONArray votesJson = new JSONArray();
        try (DbIterator<Vote> votes = Vote.getVotes(poll.getId(), firstIndex, lastIndex)) {
            for (Vote vote : votes) {
                votesJson.add(JSONData.vote(poll, vote));
            }
        }
        JSONObject response = new JSONObject();
        response.put("votes", votesJson);
        return response;
    }
}
