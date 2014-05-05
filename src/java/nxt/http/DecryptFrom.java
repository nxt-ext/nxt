package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static nxt.http.JSONResponses.DECRYPTION_FAILED;
import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;

public final class DecryptFrom extends APIServlet.APIRequestHandler {

    static final DecryptFrom instance = new DecryptFrom();

    private DecryptFrom() {
        super("account", "data", "nonce", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        if (account.getPublicKey() == null) {
            return INCORRECT_ACCOUNT;
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req);
        byte[] data = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("data")));
        byte[] nonce = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("nonce")));
        EncryptedData encryptedData = new EncryptedData(data, nonce);
        try {
            byte[] decrypted = account.decryptFrom(encryptedData, secretPhrase);
            JSONObject response = new JSONObject();
            response.put("data", Convert.toHexString(decrypted));
            response.put("text", new String(decrypted, "UTF-8"));
            return response;
        } catch (UnsupportedEncodingException | RuntimeException ignore) {
            return DECRYPTION_FAILED;
        }
    }

}
