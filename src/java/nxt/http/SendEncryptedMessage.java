package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;

public final class SendEncryptedMessage extends CreateTransaction {

    static final SendEncryptedMessage instance = new SendEncryptedMessage();

    private SendEncryptedMessage() {
        super(new APITag[] {APITag.MESSAGES, APITag.CREATE_TRANSACTION}, "recipient");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long recipientId = ParameterParser.getRecipientId(req);
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            return INCORRECT_RECIPIENT;
        }
        Account senderAccount = ParameterParser.getSenderAccount(req);
        return createTransaction(req, senderAccount, recipientId, 0, Attachment.ENCRYPTED_MESSAGE);

    }

}
