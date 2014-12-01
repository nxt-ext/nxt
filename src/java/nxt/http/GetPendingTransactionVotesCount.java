package nxt.http;


import nxt.Nxt;
import nxt.PendingTransactionPoll;
import nxt.VotePhased;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PENDING_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;

public class GetPendingTransactionVotesCount extends APIServlet.APIRequestHandler {
    static final GetPendingTransactionVotesCount instance = new GetPendingTransactionVotesCount();

    private GetPendingTransactionVotesCount() {
        super(new APITag[]{APITag.PENDING_TRANSACTIONS}, "pendingTransaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        String transactionIdString = Convert.emptyToNull(req.getParameter("pendingTransaction"));

        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }

        long transactionId = Convert.parseUnsignedLong(transactionIdString);
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
        response.put("released", poll.isFinished());
        return response;
    }
}