package nxt.http;

import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;

public final class GetAccountId extends APIServlet.APIRequestHandler {

    static final GetAccountId instance = new GetAccountId();

    private GetAccountId() {}

    private static final List<String> parameters = Arrays.asList("secretPhrase");

    @Override
    List<String> getParameters() {
        return parameters;
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

        JSONObject response = new JSONObject();
        response.put("accountId", Convert.toUnsignedLong(Account.getId(publicKey)));

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
