package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.Shuffling;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingRegister extends CreateTransaction {

    static final ShufflingRegister instance = new ShufflingRegister();

    private ShufflingRegister() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION}, "shuffling");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Shuffling shuffling = ParameterParser.getShuffling(req);
        Attachment attachment = new Attachment.MonetarySystemShufflingRegistration(shuffling.getId());

        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, attachment);
    }
}
