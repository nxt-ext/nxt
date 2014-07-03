package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;
import static nxt.http.JSONResponses.MISSING_NOTE;

public final class SendMoneyWithMessage extends CreateTransaction {

    static final SendMoneyWithMessage instance = new SendMoneyWithMessage();

    private SendMoneyWithMessage() {
        super(new APITag[] {APITag.MESSAGES, APITag.CREATE_TRANSACTION, APITag.ACCOUNTS},
                "recipient", "amountNQT", "note", "encryptedNote", "encryptedNoteNonce");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long recipientId = ParameterParser.getRecipientId(req);
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            return INCORRECT_RECIPIENT;
        }
        EncryptedData encryptedMessage = ParameterParser.getEncryptedNote(req, recipientAccount);
        if (encryptedMessage == EncryptedData.EMPTY_DATA) {
            return MISSING_NOTE;
        }
        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.PaymentMessage(encryptedMessage);
        long amountNQT = ParameterParser.getAmountNQT(req);
        return createTransaction(req, senderAccount, recipientId, amountNQT, attachment);
    }

}
