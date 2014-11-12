package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class LeaseBalance extends CreateTransaction {

    static final LeaseBalance instance = new LeaseBalance();

    private LeaseBalance() {
        super(new APITag[] {APITag.FORGING}, "period", "recipient");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        short period = (short)ParameterParser.getInt(req, "period", Constants.MIN_LEASING_WAITING_PERIOD, Short.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);
        long recipient = ParameterParser.getRecipientId(req);
        Account recipientAccount = Account.getAccount(recipient);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "recipient account does not have public key");
            return response;
        }
        Attachment attachment = new Attachment.AccountControlEffectiveBalanceLeasing(period);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
