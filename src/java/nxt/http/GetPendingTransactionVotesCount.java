package nxt.http;


import nxt.Nxt;
import nxt.NxtException;
import nxt.PendingTransactionPoll;
import nxt.VotePhased;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;

public class GetPendingTransactionVotesCount extends APIServlet.APIRequestHandler {
    static final GetPendingTransactionVotesCount instance = new GetPendingTransactionVotesCount();

    private GetPendingTransactionVotesCount() {
        super(new APITag[]{APITag.PENDING_TRANSACTIONS}, "pendingTransaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long transactionId = ParameterParser.getLong(req, "pendingTransaction", Long.MIN_VALUE, Long.MAX_VALUE, true);
        PendingTransactionPoll poll = PendingTransactionPoll.getPoll(transactionId);
        if(poll == null){
            return  INCORRECT_PENDING_TRANSACTION;
        }

        int count = VotePhased.getCount(transactionId);
        long quorum = poll.getQuorum();

        JSONObject response = new JSONObject();
        response.put("votesCount", count);
        response.put("quorum", quorum);
        response.put("refusalHeight", poll.getFinishBlockHeight());
        response.put("height", Nxt.getBlockchain().getHeight());
        return response;
    }
}