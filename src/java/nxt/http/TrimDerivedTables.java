package nxt.http;

import nxt.Nxt;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class TrimDerivedTables extends APIServlet.APIRequestHandler {

    static final TrimDerivedTables instance = new TrimDerivedTables();

    private TrimDerivedTables() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        Nxt.getBlockchainProcessor().trimDerivedTables();
        response.put("done", true);
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

    @Override
    boolean requirePassword() {
        return true;
    }
}
