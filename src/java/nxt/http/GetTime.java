package nxt.http;

import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetTime extends HttpRequestHandler {

    static final GetTime instance = new GetTime();

    private GetTime() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("time", Convert.getEpochTime());

        return response;
    }

}
