package nxt.http;

import nxt.Account;
import nxt.Generator;
import nxt.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_FORGING;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;


public final class GetForging extends APIServlet.APIRequestHandler {

    static final GetForging instance = new GetForging();

    private GetForging() {
        super("secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }
        Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Generator generator = Generator.getGenerator(secretPhrase);
        if (generator == null) {
            return NOT_FORGING;
        }

        JSONObject response = new JSONObject();
        response.put("deadline", generator.getDeadline());
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
