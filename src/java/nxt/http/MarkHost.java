package nxt.http;

import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

final class MarkHost extends HttpRequestHandler {

    static final MarkHost instance = new MarkHost();

    private MarkHost() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String secretPhrase = req.getParameter("secretPhrase");
        String host = req.getParameter("host");
        String weightValue = req.getParameter("weight");
        String dateValue = req.getParameter("date");
        if (secretPhrase == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"secretPhrase\" not specified");

        } else if (host == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"host\" not specified");

        } else if (weightValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"weight\" not specified");

        } else if (dateValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"date\" not specified");

        } else {

            if (host.length() > 100) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"host\" (the length exceeds 100 chars limit)");

            } else {

                try {

                    int weight = Integer.parseInt(weightValue);
                    if (weight <= 0 || weight > Nxt.MAX_BALANCE) {

                        throw new Exception();

                    }

                    try {

                        int date = Integer.parseInt(dateValue.substring(0, 4)) * 10000 + Integer.parseInt(dateValue.substring(5, 7)) * 100 + Integer.parseInt(dateValue.substring(8, 10));

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

                        response.put("hallmark", Convert.convert(data) + Convert.convert(signature));

                    } catch (Exception e) {

                        response.put("errorCode", 4);
                        response.put("errorDescription", "Incorrect \"date\"");

                    }

                } catch (Exception e) {

                    response.put("errorCode", 4);
                    response.put("errorDescription", "Incorrect \"weight\"");

                }

            }

        }
        return response;
    }

}
