package nxt.http;

import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static nxt.http.JSONResponses.INCORRECT_TOKEN;
import static nxt.http.JSONResponses.INCORRECT_WEBSITE;
import static nxt.http.JSONResponses.MISSING_TOKEN;
import static nxt.http.JSONResponses.MISSING_WEBSITE;

public final class DecodeToken extends HttpRequestHandler {

    static final DecodeToken instance = new DecodeToken();

    private DecodeToken() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String website = req.getParameter("website");
        String token = req.getParameter("token");
        if (website == null) {
            return MISSING_WEBSITE;
        } else if (token == null) {
            return MISSING_TOKEN;
        }

        try {
            byte[] websiteBytes = website.trim().getBytes("UTF-8");
            byte[] tokenBytes = new byte[100];
            int i = 0, j = 0;
            try {

                for (; i < token.length(); i += 8, j += 5) {

                    long number = Long.parseLong(token.substring(i, i + 8), 32);
                    tokenBytes[j] = (byte)number;
                    tokenBytes[j + 1] = (byte)(number >> 8);
                    tokenBytes[j + 2] = (byte)(number >> 16);
                    tokenBytes[j + 3] = (byte)(number >> 24);
                    tokenBytes[j + 4] = (byte)(number >> 32);

                }

            } catch (NumberFormatException e) {
                return INCORRECT_TOKEN;
            }

            if (i != 160) {
                return INCORRECT_TOKEN;
            }
            byte[] publicKey = new byte[32];
            System.arraycopy(tokenBytes, 0, publicKey, 0, 32);
            int timestamp = (tokenBytes[32] & 0xFF) | ((tokenBytes[33] & 0xFF) << 8) | ((tokenBytes[34] & 0xFF) << 16) | ((tokenBytes[35] & 0xFF) << 24);
            byte[] signature = new byte[64];
            System.arraycopy(tokenBytes, 36, signature, 0, 64);

            byte[] data = new byte[websiteBytes.length + 36];
            System.arraycopy(websiteBytes, 0, data, 0, websiteBytes.length);
            System.arraycopy(tokenBytes, 0, data, websiteBytes.length, 36);
            boolean valid = Crypto.verify(signature, data, publicKey);

            JSONObject response = new JSONObject();
            response.put("account", Convert.convert(Account.getId(publicKey)));
            response.put("timestamp", timestamp);
            response.put("valid", valid);

            return response;

        } catch (RuntimeException|UnsupportedEncodingException e) {
            return INCORRECT_WEBSITE;
        }
    }

}
