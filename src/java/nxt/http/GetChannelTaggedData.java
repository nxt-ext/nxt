package nxt.http;

import nxt.NxtException;
import nxt.TaggedData;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetChannelTaggedData extends APIServlet.APIRequestHandler {

    static final GetChannelTaggedData instance = new GetChannelTaggedData();

    private GetChannelTaggedData() {
        super(new APITag[] {APITag.DATA}, "channel", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String channel = Convert.emptyToNull(req.getParameter("channel"));
        if (channel == null) {
            return JSONResponses.missing("channel");
        }
        long accountId = ParameterParser.getAccountId(req, "account", false);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("data", jsonArray);
        try (DbIterator<TaggedData> data = TaggedData.getData(channel, accountId, firstIndex, lastIndex)) {
            while (data.hasNext()) {
                jsonArray.add(JSONData.taggedData(data.next()));
            }
        }
        return response;
    }

}