package nxt.http;

import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class DecodeToken extends HttpRequestHandler {

    static final DecodeToken instance = new DecodeToken();

    private DecodeToken() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        String website = req.getParameter("website");
        String token = req.getParameter("token");
        if (website == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"website\" not specified");

        } else if (token == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"token\" not specified");

        } else {

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

                } catch (Exception e) { }

                if (i != 160) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"token\"");

                } else {

                    byte[] publicKey = new byte[32];
                    System.arraycopy(tokenBytes, 0, publicKey, 0, 32);
                    int timestamp = (tokenBytes[32] & 0xFF) | ((tokenBytes[33] & 0xFF) << 8) | ((tokenBytes[34] & 0xFF) << 16) | ((tokenBytes[35] & 0xFF) << 24);
                    byte[] signature = new byte[64];
                    System.arraycopy(tokenBytes, 36, signature, 0, 64);

                    byte[] data = new byte[websiteBytes.length + 36];
                    System.arraycopy(websiteBytes, 0, data, 0, websiteBytes.length);
                    System.arraycopy(tokenBytes, 0, data, websiteBytes.length, 36);
                    boolean valid = Crypto.verify(signature, data, publicKey);

                    response.put("account", Convert.convert(Account.getId(publicKey)));
                    response.put("timestamp", timestamp);
                    response.put("valid", valid);

                }
            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"website\"");

            }

        }
        return response;
    }

}
