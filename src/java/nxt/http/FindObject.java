package nxt.http;

import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_GUID;
import static nxt.http.JSONResponses.MISSING_GUID;

public final class FindObject extends APIServlet.APIRequestHandler {

    static final FindObject instance = new FindObject();

    private FindObject() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String guid = req.getParameter("guid");
        if (guid == null) {
            return MISSING_GUID;
        }

        try {

            JSONObject response = new JSONObject();

            Long objectId;
            byte[] guidBytes = Convert.parseHexString(guid);
            if ((guidBytes[guidBytes.length - 1] & 0x80) == 0) {
                response.put("type", "transaction");
                objectId = Nxt.getTransactionProcessor().findTransaction(Convert.toHexString(guidBytes));
            } else {
                response.put("type", "unknown");
                objectId = null;
            }

            response.put("id", Convert.toUnsignedLong(objectId));
            return response;

        } catch (RuntimeException e) {
            return INCORRECT_GUID;
        }

    }

}
