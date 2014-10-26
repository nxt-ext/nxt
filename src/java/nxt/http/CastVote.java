package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;
import static nxt.http.JSONResponses.INCORRECT_POLL;
import static nxt.http.JSONResponses.INCORRECT_VOTE;
import static nxt.http.JSONResponses.MISSING_POLL;


public final class CastVote extends CreateTransaction {

    static final CastVote instance = new CastVote();

    private CastVote() {
        super(new APITag[] {APITag.VS, APITag.CREATE_TRANSACTION}, "poll", "vote1", "vote2", "vote3"); // todo: fix hardcoded to 3 votes for testing  todo: fix ??
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String pollValue = req.getParameter("poll");

        if (pollValue == null) {
            return MISSING_POLL;
        }

        Poll poll;
        try {
            long pollId = Convert.parseUnsignedLong(pollValue);
            poll = Poll.getPoll(pollId);
            if (poll == null || Poll.isFinished(pollId)) {
                return INCORRECT_POLL;
            }
        } catch (RuntimeException e) {
            return INCORRECT_POLL;
        }

        int numberOfOptions = poll.getOptions().length;
        byte[] vote = new byte[numberOfOptions];
        try {
            for (int i = 0; i < numberOfOptions; i++) {
                String voteValue = req.getParameter("vote" + i);
                if (voteValue != null) {
                    vote[i] = Byte.parseByte(voteValue);
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
