package nxt.http;

import nxt.*;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_POLL;
import static nxt.http.JSONResponses.INCORRECT_VOTE;
import static nxt.http.JSONResponses.MISSING_POLL;


public final class CastVote extends CreateTransaction {

    static final CastVote instance = new CastVote();

    private CastVote() {
        super(new APITag[]{APITag.VS, APITag.CREATE_TRANSACTION}, "poll", "vote1", "vote2", "vote3");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long pollId = ParameterParser.getLong(req, "poll", Long.MIN_VALUE, Long.MAX_VALUE, true);
        Poll poll = Poll.getPoll(pollId);
        if (poll == null || poll.isFinished()) {
            return INCORRECT_POLL;
        }


        int numberOfOptions = poll.getOptions().length;
        byte[] vote = new byte[numberOfOptions];
        //todo: optimize with ParameterParser?
        try {
            for (int i = 1; i <= numberOfOptions; i++) {
                String voteValue = req.getParameter("vote" + i);
                if (voteValue != null) {
                    vote[i - 1] = Byte.parseByte(voteValue);
                } else {
                    vote[i - 1] = Constants.VOTING_NO_VOTE_VALUE;
                }
            }
        } catch (NumberFormatException e) {
            return INCORRECT_VOTE;
        }

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingVoteCasting(poll.getId(), vote);
        return createTransaction(req, account, attachment);
    }
}
