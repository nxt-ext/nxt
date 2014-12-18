package nxt.http;


import nxt.NxtException;
import nxt.Poll;
import nxt.Vote;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import java.util.List;



public class GetPollVotes extends APIServlet.APIRequestHandler  {
    static final GetPollVotes instance = new GetPollVotes();

    private GetPollVotes() { super(new APITag[] {APITag.VS}, "poll"); }


    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        Poll poll = ParameterParser.getPoll(req);

        List<Vote> votes = Vote.getVotes(poll.getId(), firstIndex, lastIndex).toList();

        JSONArray votesJson = new JSONArray();
        for (Vote vote : votes) {
            votesJson.add(JSONData.vote(poll, vote));
        }

        JSONObject response = new JSONObject();
        response.put("votes", votesJson);
        return response;
    }
}
