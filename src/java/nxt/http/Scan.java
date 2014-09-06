package nxt.http;

import nxt.Nxt;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class Scan extends APIServlet.APIRequestHandler {

    static final Scan instance = new Scan();

    private Scan() {
        super(new APITag[] {APITag.DEBUG}, "height", "validate");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            if ("true".equalsIgnoreCase(req.getParameter("validate"))) {
                Nxt.getBlockchainProcessor().validateAtNextScan();
            }
            int height = 0;
            try {
                height = Integer.parseInt(req.getParameter("height"));
            } catch (NumberFormatException ignore) {}
            long start = System.currentTimeMillis();
            Nxt.getBlockchainProcessor().scan(height);
            long end = System.currentTimeMillis();
            response.put("done", true);
            response.put("scanTime", (end - start)/1000);
        } catch (RuntimeException e) {
            response.put("error", e.toString());
        }
        return response;
    }

}
