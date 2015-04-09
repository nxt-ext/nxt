package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.PrunableMessage;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPrunableMessage extends APIServlet.APIRequestHandler {

    static final GetPrunableMessage instance = new GetPrunableMessage();

    private GetPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        long readerAccountId = secretPhrase == null ? 0 : Account.getId(Crypto.getPublicKey(secretPhrase));
        PrunableMessage prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
        JSONObject response = JSONData.prunableMessage(prunableMessage, readerAccountId, secretPhrase);
        return response;
    }

}
