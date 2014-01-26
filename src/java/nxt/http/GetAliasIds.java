package nxt.http;

import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;

final class GetAliasIds extends HttpRequestHandler {

    static final GetAliasIds instance = new GetAliasIds();

    private GetAliasIds() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        String timestampValue = req.getParameter("timestamp");
        if (timestampValue == null) {

            response.put("errorCode", 3);
            response.put("errorDescription", "\"timestamp\" not specified");

        } else {

            try {

                int timestamp = Integer.parseInt(timestampValue);
                if (timestamp < 0) {

                    throw new Exception();

                }

                JSONArray aliasIds = new JSONArray();
                for (Alias alias : Alias.allAliases) {
                    if (alias.timestamp >= timestamp) {
                        aliasIds.add(Convert.convert(alias.id));
                    }
                }

                response.put("aliasIds", aliasIds);

            } catch (Exception e) {

                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"timestamp\"");

            }

        }
        return response;
    }

}
