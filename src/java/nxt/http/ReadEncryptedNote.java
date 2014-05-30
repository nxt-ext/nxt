package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionType;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.DECRYPTION_FAILED;
import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class ReadEncryptedNote extends APIServlet.APIRequestHandler {

    static final ReadEncryptedNote instance = new ReadEncryptedNote();

    private ReadEncryptedNote() {
        super("transaction", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }

        Transaction transaction;
        try {
            transaction = Nxt.getBlockchain().getTransaction(Convert.parseUnsignedLong(transactionIdString));
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }
        if (transaction.getType() != TransactionType.Messaging.ENCRYPTED_MESSAGE) {
            return INCORRECT_TRANSACTION;
        }

        String secretPhrase = ParameterParser.getSecretPhrase(req);
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        Attachment.MessagingEncryptedMessage attachment = (Attachment.MessagingEncryptedMessage)transaction.getAttachment();
        try {
            byte[] decrypted = senderAccount.decryptFrom(attachment.getEncryptedMessage(), secretPhrase);
            JSONObject response = new JSONObject();
            response.put("note", Convert.toString(decrypted));
            return response;
        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString(), e);
            return DECRYPTION_FAILED;
        }

    }

}
