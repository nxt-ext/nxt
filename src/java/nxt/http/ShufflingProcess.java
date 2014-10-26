package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Shuffling;
import nxt.ShufflingParticipant;
import nxt.db.DbIterator;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_PUBLIC_KEY;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;

public final class ShufflingProcess extends CreateTransaction {

    static final ShufflingProcess instance = new ShufflingProcess();

    private ShufflingProcess() {
        super(new APITag[] {APITag.SHUFFLE, APITag.CREATE_TRANSACTION},
                "shuffling", "sourceSecretPhrase", "destinationPublicKey");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        String secretPhrase = req.getParameter("sourceSecretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }
        String destinationPublicKey = req.getParameter("destinationPublicKey");
        if (secretPhrase == null) {
            return MISSING_PUBLIC_KEY;
        }
        ShufflingParticipant participant = ShufflingParticipant.getParticipant(shuffling.getId(), shuffling.getAssigneeAccountId());

        // Here comes the fun part ...


//        Attachment attachment = new Attachment.MonetarySystemShufflingDistribution(shuffling.getId());
//
//        Account account = ParameterParser.getSenderAccount(req);
//        return createTransaction(req, account, attachment);
        return null;
    }
}
