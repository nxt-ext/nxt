package nxt.http;

import nxt.Appendix;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class VerifyPrunableMessage extends APIServlet.APIRequestHandler {

    static final VerifyPrunableMessage instance = new VerifyPrunableMessage();

    private static final JSONStreamAware NO_SUCH_PLAIN_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no plain message attachment");
        NO_SUCH_PLAIN_MESSAGE = JSON.prepare(response);
    }

    private static final JSONStreamAware NO_SUCH_ENCRYPTED_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no encrypted message attachment");
        NO_SUCH_ENCRYPTED_MESSAGE = JSON.prepare(response);
    }

    private static final JSONStreamAware TOO_MANY_MESSAGES;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 8);
        response.put("errorDescription", "Cannot have both plain and encrypted message");
        TOO_MANY_MESSAGES = JSON.prepare(response);
    }

    public static final JSONStreamAware HASHES_MISMATCH;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 10);
        response.put("errorDescription", "Hashes don't match. You should notify Jeff Garzik.");
        HASHES_MISMATCH = JSON.prepare(response);
    }

    private VerifyPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction",
                "message", "messageIsText", "messageIsPrunable",
                "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = Nxt.getBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        Appendix.PrunablePlainMessage plainMessage = ParameterParser.getPrunablePlainMessage(req);
        Appendix.PrunableEncryptedMessage encryptedMessage = ParameterParser.getPrunableEncryptedMessage(req);

        if (plainMessage == null && encryptedMessage == null) {
            return JSONResponses.missing("message", "encryptedMessageData");
        }
        if (plainMessage != null && encryptedMessage != null) {
            return TOO_MANY_MESSAGES;
        }

        if (plainMessage != null) {
            Appendix.PrunablePlainMessage myPlainMessage = transaction.getPrunablePlainMessage();
            if (myPlainMessage == null) {
                return NO_SUCH_PLAIN_MESSAGE;
            }
            if (!Arrays.equals(myPlainMessage.getHash(), plainMessage.getHash())) {
                return HASHES_MISMATCH;
            }
            JSONObject response = myPlainMessage.getJSONObject();
            response.put("messageHash", myPlainMessage.getHash());
            response.put("verify", true);
            return response;
        } else if (encryptedMessage != null) {
            Appendix.PrunableEncryptedMessage myEncryptedMessage = transaction.getPrunableEncryptedMessage();
            if (myEncryptedMessage == null) {
                return NO_SUCH_ENCRYPTED_MESSAGE;
            }
            if (!Arrays.equals(myEncryptedMessage.getHash(), encryptedMessage.getHash())) {
                return HASHES_MISMATCH;
            }
            JSONObject response = myEncryptedMessage.getJSONObject();
            response.put("encryptedMessageHash", myEncryptedMessage.getHash());
            response.put("verify", true);
            return response;
        }

        return JSON.emptyJSON;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
