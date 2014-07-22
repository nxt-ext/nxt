package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;

public final class SendEncryptedNote extends CreateTransaction {

    static final SendEncryptedNote instance = new SendEncryptedNote();

    private SendEncryptedNote() {
        super(new APITag[] {APITag.MESSAGES, APITag.CREATE_TRANSACTION}, "recipient", "note", "encryptedNote", "encryptedNoteNonce");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long recipientId = ParameterParser.getRecipientId(req);
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            return INCORRECT_RECIPIENT;
        }
        EncryptedData encryptedMessage = ParameterParser.getEncryptedNote(req, recipientAccount);
        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingEncryptedMessage(encryptedMessage);
        return createTransaction(req, senderAccount, recipientId, 0, attachment);

    }

}
