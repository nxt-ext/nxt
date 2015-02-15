package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.PhasingPoll;
import nxt.PhasingVote;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPhasingVotes extends APIServlet.APIRequestHandler {
    static final GetPhasingVotes instance = new GetPhasingVotes();

    private GetPhasingVotes() {
        super(new APITag[]{APITag.PHASING}, "transaction", "includeVoters");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        PhasingPoll poll = ParameterParser.getPhasingPoll(req);
        boolean includeVoters = ParameterParser.getBoolean(req, "includeVoters", false);

        long votesTotal = PhasingVote.countVotes(poll);
        long quorum = poll.getQuorum();

        JSONObject response = new JSONObject();
        response.put("transaction", Convert.toUnsignedLong(poll.getId()));
        response.put("votes", votesTotal);
        response.put("quorum", quorum);
        response.put("finishHeight", poll.getFinishHeight());
        response.put("finished", poll.isFinished());
        response.put("height", Nxt.getBlockchain().getHeight());

        if (includeVoters) {
            JSONArray votersJson = new JSONArray();
            try (DbIterator<PhasingVote> votes = PhasingVote.getByTransaction(poll.getId(), 0, Integer.MAX_VALUE)) {
                for (PhasingVote vote : votes) {
                    JSONObject voterObject = new JSONObject();
                    JSONData.putAccount(voterObject, "voter", vote.getVoterId());
                    votersJson.add(voterObject);
                }
            }
            response.put("voters", votersJson);
        }

        return response;
    }
}