package nxt.http;

import nxt.Account;
import nxt.Appendix;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.DECRYPTION_FAILED;
import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.NO_MESSAGE;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class ReadMessage extends APIServlet.APIRequestHandler {

    static final ReadMessage instance = new ReadMessage();

    private ReadMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase");
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

        JSONObject response = new JSONObject();
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        Appendix.Message message = transaction.getMessage();
        Appendix.EncryptedMessage encryptedMessage = transaction.getEncryptedMessage();
        if (message == null && encryptedMessage == null) {
            return NO_MESSAGE;
        }
        if (encryptedMessage != null) {
            String secretPhrase = ParameterParser.getSecretPhrase(req);
            try {
                byte[] decrypted = senderAccount.decryptFrom(encryptedMessage.getEncryptedData(), secretPhrase);
                response.put("decryptedMessage", encryptedMessage.isText() ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
            } catch (RuntimeException e) {
                Logger.logDebugMessage(e.toString(), e);
                return DECRYPTION_FAILED;
            }
        }
        if (message != null) {
            response.put("message", message.isText() ? Convert.toString(message.getMessage()) : Convert.toHexString(message.getMessage()));
        }
        return response;
    }

}
