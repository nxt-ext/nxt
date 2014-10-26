package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Shuffling;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingVerify extends CreateTransaction {

    static final ShufflingVerify instance = new ShufflingVerify();

    private ShufflingVerify() {
        super(new APITag[] {APITag.SHUFFLE, APITag.CREATE_TRANSACTION}, "shuffling");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        Attachment attachment = new Attachment.MonetarySystemShufflingVerification(shuffling.getId());

        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, attachment);
    }
}
