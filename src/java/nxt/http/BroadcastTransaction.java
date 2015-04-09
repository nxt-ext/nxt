package nxt.http;

import nxt.Appendix;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionBytes", "transactionJSON",
                "message", "messageIsText", "messageIsPrunable",
                "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));

        Transaction.Builder builder = ParameterParser.parseTransaction(transactionBytes, transactionJSON);

        String messageValue = Convert.emptyToNull(req.getParameter("message"));
        if (messageValue != null) {
            boolean messageIsText = !"false".equalsIgnoreCase(req.getParameter("messageIsText"));
            boolean messageIsPrunable = "true".equalsIgnoreCase(req.getParameter("messageIsPrunable"));
            if (messageIsPrunable) {
                try {
                    builder.appendix(new Appendix.PrunablePlainMessage(messageValue, messageIsText));
                } catch (RuntimeException e) {
                    return INCORRECT_ARBITRARY_MESSAGE;
                }
            }
        }

        EncryptedData encryptedData = ParameterParser.getEncryptedMessage(req, null);
        boolean encryptedDataIsText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText"));
        if (encryptedData != null) {
            if ("true".equalsIgnoreCase(req.getParameter("encryptedMessageIsPrunable"))) {
                builder.appendix(new Appendix.PrunableEncryptedMessage(encryptedData, encryptedDataIsText));
            }
        }

        Transaction transaction = builder.build();

        JSONObject response = new JSONObject();
        try {
            Nxt.getTransactionProcessor().broadcast(transaction);
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHash());
        } catch (NxtException.ValidationException|RuntimeException e) {
            Logger.logDebugMessage(e.getMessage(), e);
            JSONData.putException(response, e);
        }
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
