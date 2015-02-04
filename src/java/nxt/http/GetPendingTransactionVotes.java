package nxt.http;

import nxt.*;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;

public class GetPendingTransactionVotes extends APIServlet.APIRequestHandler {
    static final GetPendingTransactionVotes instance = new GetPendingTransactionVotes();

    private GetPendingTransactionVotes() {
        super(new APITag[]{APITag.PENDING_TRANSACTIONS}, "pendingTransaction", "includeVoters");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        PendingTransactionPoll poll = ParameterParser.getPendingTransactionPoll(req);
        boolean includeVoters = ParameterParser.getBoolean(req, "includeVoters", false);

        long votesTotal = VotePhased.countVotes(poll);
        long quorum = poll.getQuorum();

        JSONObject response = new JSONObject();
        response.put("pendingTransaction", Convert.toUnsignedLong(poll.getId()));
        response.put("votes", votesTotal);
        response.put("quorum", quorum);
        response.put("finishHeight", poll.getFinishHeight());
        response.put("finished", poll.isFinished());
        response.put("height", Nxt.getBlockchain().getHeight());

        if (includeVoters) {
            JSONArray votersJson = new JSONArray();
            try (DbIterator<VotePhased> votes = VotePhased.getByTransaction(poll.getId(), 0, Integer.MAX_VALUE)) {
                for (VotePhased vote : votes) {
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