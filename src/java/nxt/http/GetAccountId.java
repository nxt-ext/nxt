package nxt.http;

import nxt.crypto.Crypto;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

final class GetAccountId extends HttpRequestHandler {

    static final GetAccountId instance = new GetAccountId();

    private GetAccountId() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else {

            byte[] publicKeyHash = Crypto.sha256().digest(Crypto.getPublicKey(secretPhrase));
            BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5],
                    publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
            response.put("accountId", bigInteger.toString());

        }
        return response;
    }

}
