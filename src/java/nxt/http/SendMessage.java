package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendMessage extends CreateTransaction {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {
        super(new APITag[] {APITag.MESSAGES, APITag.CREATE_TRANSACTION}, "recipient");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        long recipientId = recipientValue != null ? ParameterParser.getRecipientId(req) : 0;
        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, recipientId, 0, Attachment.ARBITRARY_MESSAGE);
    }

}
