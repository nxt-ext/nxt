package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;

public final class DecryptFrom extends APIServlet.APIRequestHandler {

    static final DecryptFrom instance = new DecryptFrom();

    private DecryptFrom() {
        super("account", "data", "nonce");
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
        byte[] decrypted = account.decryptFrom(encryptedData, secretPhrase);
        JSONObject response = new JSONObject();
        response.put("data", Convert.toHexString(decrypted));
        try {
            response.put("text", new String(data, "UTF-8"));
        } catch (UnsupportedEncodingException|RuntimeException ignore) {}
        return response;

    }

}
