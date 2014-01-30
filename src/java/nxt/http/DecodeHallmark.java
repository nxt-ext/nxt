package nxt.http;

import nxt.Account;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static nxt.http.JSONResponses.INCORRECT_HALLMARK;
import static nxt.http.JSONResponses.MISSING_HALLMARK;

final class DecodeHallmark extends HttpRequestHandler {

    static final DecodeHallmark instance = new DecodeHallmark();

    private DecodeHallmark() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String hallmarkValue = req.getParameter("hallmark");
        if (hallmarkValue == null) {
            return MISSING_HALLMARK;
        }

        try {
            byte[] hallmark = Convert.convert(hallmarkValue);

            ByteBuffer buffer = ByteBuffer.wrap(hallmark);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            byte[] publicKey = new byte[32];
            buffer.get(publicKey);
            int hostLength = buffer.getShort();
            if (hostLength > 300) {
                throw new IllegalArgumentException("Invalid host length");
            }
            byte[] hostBytes = new byte[hostLength];
            buffer.get(hostBytes);
            String host = new String(hostBytes, "UTF-8");
            int weight = buffer.getInt();
            int date = buffer.getInt();
            buffer.get();
            byte[] signature = new byte[64];
            buffer.get(signature);

            JSONObject response = new JSONObject();
            response.put("account", Convert.convert(Account.getId(publicKey)));
            response.put("host", host);
            response.put("weight", weight);
            int year = date / 10000;
            int month = (date % 10000) / 100;
            int day = date % 100;
            response.put("date", (year < 10 ? "000" : (year < 100 ? "00" : (year < 1000 ? "0" : ""))) + year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day);
            byte[] data = new byte[hallmark.length - 64];
            System.arraycopy(hallmark, 0, data, 0, data.length);
            response.put("valid", host.length() > 100 || weight <= 0 || weight > Nxt.MAX_BALANCE ? false : Crypto.verify(signature, data, publicKey));
            return response;
        } catch (RuntimeException|UnsupportedEncodingException e) {
            return INCORRECT_HALLMARK;
        }
    }

}
