package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.PendingTransactionPoll;
import nxt.VotePhased;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;

public class GetPendingTransactionVotes extends APIServlet.APIRequestHandler {
    static final GetPendingTransactionVotes instance = new GetPendingTransactionVotes();

    private GetPendingTransactionVotes() {
        super(new APITag[]{APITag.PENDING_TRANSACTIONS}, "pendingTransaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        PendingTransactionPoll poll = ParameterParser.getPendingTransactionPoll(req);

        long votes = VotePhased.countVotes(poll);
        long quorum = poll.getQuorum();

        JSONObject response = new JSONObject();
        response.put("pendingTransaction", Convert.toUnsignedLong(poll.getId()));
        response.put("votes", votes);
        response.put("quorum", quorum);
        response.put("finishHeight", poll.getFinishHeight());
        response.put("height", Nxt.getBlockchain().getHeight());
        return response;
    }
}