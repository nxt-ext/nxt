package nxt.http;

import nxt.Account;
import nxt.peer.Hallmark;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_HALLMARK;
import static nxt.http.JSONResponses.MISSING_HALLMARK;

public final class DecodeHallmark extends APIServlet.APIRequestHandler {

    static final DecodeHallmark instance = new DecodeHallmark();

    private DecodeHallmark() {
        super("hallmark");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String hallmarkValue = req.getParameter("hallmark");
        if (hallmarkValue == null) {
            return MISSING_HALLMARK;
        }

        try {

            Hallmark hallmark = Hallmark.parseHallmark(hallmarkValue);

            JSONObject response = new JSONObject();
            response.put("account", Convert.toUnsignedLong(Account.getId(hallmark.getPublicKey())));
            response.put("host", hallmark.getHost());
            response.put("weight", hallmark.getWeight());
            String dateString = Hallmark.formatDate(hallmark.getDate());
            response.put("date", dateString);
            response.put("valid", hallmark.isValid());

            return response;

        } catch (RuntimeException e) {
            return INCORRECT_HALLMARK;
        }
    }

}
