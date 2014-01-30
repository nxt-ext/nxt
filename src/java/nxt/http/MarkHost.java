package nxt.http;

import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

import static nxt.http.JSONResponses.INCORRECT_DATE;
import static nxt.http.JSONResponses.INCORRECT_HOST;
import static nxt.http.JSONResponses.INCORRECT_WEIGHT;
import static nxt.http.JSONResponses.MISSING_DATE;
import static nxt.http.JSONResponses.MISSING_HOST;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.MISSING_WEIGHT;


final class MarkHost extends HttpRequestHandler {

    static final MarkHost instance = new MarkHost();

    private MarkHost() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        String host = req.getParameter("host");
        String weightValue = req.getParameter("weight");
        String dateValue = req.getParameter("date");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (host == null) {
            return MISSING_HOST;
        } else if (weightValue == null) {
            return MISSING_WEIGHT;
        } else if (dateValue == null) {
            return MISSING_DATE;
        }

        if (host.length() > 100) {
            return INCORRECT_HOST;
        }

        int weight;
        try {
            weight = Integer.parseInt(weightValue);
            if (weight <= 0 || weight > Nxt.MAX_BALANCE) {
                return INCORRECT_WEIGHT;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_WEIGHT;
        }

        int date;
        try {
            date = Integer.parseInt(dateValue.substring(0, 4)) * 10000 + Integer.parseInt(dateValue.substring(5, 7)) * 100 + Integer.parseInt(dateValue.substring(8, 10));
        } catch (NumberFormatException e) {
            return INCORRECT_DATE;
        }

        try {

            byte[] publicKey = Crypto.getPublicKey(secretPhrase);
            byte[] hostBytes = host.getBytes("UTF-8");

            ByteBuffer buffer = ByteBuffer.allocate(32 + 2 + hostBytes.length + 4 + 4 + 1);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(publicKey);
            buffer.putShort((short)hostBytes.length);
            buffer.put(hostBytes);
            buffer.putInt(weight);
            buffer.putInt(date);

            byte[] data = buffer.array();
            byte[] signature;
            do {
                data[data.length - 1] = (byte) ThreadLocalRandom.current().nextInt();
                signature = Crypto.sign(data, secretPhrase);
            } while (!Crypto.verify(signature, data, publicKey));

            JSONObject response = new JSONObject();
            response.put("hallmark", Convert.convert(data) + Convert.convert(signature));
            return response;

        } catch (RuntimeException|UnsupportedEncodingException e) {
            return INCORRECT_HOST;
        }

    }

}
