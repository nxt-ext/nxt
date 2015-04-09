package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.PrunableMessage;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPrunableMessages extends APIServlet.APIRequestHandler {

    static final GetPrunableMessages instance = new GetPrunableMessages();

    private GetPrunableMessages() {
        super(new APITag[] {APITag.MESSAGES}, "account", "secretPhrase", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        long readerAccountId = secretPhrase == null ? 0 : Account.getId(Crypto.getPublicKey(secretPhrase));
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("prunableMessages", jsonArray);
        try (DbIterator<PrunableMessage> iterator = PrunableMessage.getPrunableMessages(account.getId(), firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                PrunableMessage prunableMessage = iterator.next();
                jsonArray.add(JSONData.prunableMessage(prunableMessage, readerAccountId, secretPhrase));
            }
        }
        return response;
    }

}
