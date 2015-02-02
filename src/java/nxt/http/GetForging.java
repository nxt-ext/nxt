package nxt.http;

import nxt.Account;
import nxt.Generator;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_FORGING;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;


public final class GetForging extends APIServlet.APIRequestHandler {

    static final GetForging instance = new GetForging();

    private GetForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase", "adminPassword");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        int elapsedTime = Nxt.getEpochTime() - Nxt.getBlockchain().getLastBlock().getTimestamp();
        if (secretPhrase != null) {
            Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
            if (account == null) {
                return UNKNOWN_ACCOUNT;
            }
            Generator generator = Generator.getGenerator(secretPhrase);
            if (generator == null) {
                return NOT_FORGING;
            }
            return JSONData.generator(generator, elapsedTime);
        } else {
            API.verifyPassword(req);
            JSONObject response = new JSONObject();
            JSONArray generators = new JSONArray();
            for (Generator generator : Generator.getSortedForgers()) {
                generators.add(JSONData.generator(generator, elapsedTime));
            }
            response.put("generators", generators);
            return response;
        }
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
