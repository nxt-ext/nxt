package nxt.http;

import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetMyInfo extends HttpRequestHandler {

    static final GetMyInfo instance = new GetMyInfo();

    private GetMyInfo() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        response.put("host", req.getRemoteHost());
        response.put("address", req.getRemoteAddr());
        return response;
    }

}
