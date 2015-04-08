package nxt.http;

import nxt.Appendix;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PRUNABLE_MESSAGE;

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionBytes", "transactionJSON", "message", "messageIsText", "messageIsPrunable");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        Appendix.PrunableMessageAppendix prunableMessageAppendix = null;
        String messageValue = Convert.emptyToNull(req.getParameter("message"));
        if (messageValue != null) {
            boolean messageIsText = !"false".equalsIgnoreCase(req.getParameter("messageIsText"));
            boolean messageIsPrunable = "true".equalsIgnoreCase(req.getParameter("messageIsPrunable"));
            if (messageIsPrunable) {
                try {
                    prunableMessageAppendix = messageIsText ? new Appendix.PrunableMessageAppendix(messageValue)
                            : new Appendix.PrunableMessageAppendix(Convert.parseHexString(messageValue));
                } catch (RuntimeException e) {
                    throw new ParameterException(INCORRECT_PRUNABLE_MESSAGE);
                }
            }
        }
        Transaction.Builder builder = ParameterParser.parseTransaction(transactionBytes, transactionJSON);
        builder.prunableMessage(prunableMessageAppendix);
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
