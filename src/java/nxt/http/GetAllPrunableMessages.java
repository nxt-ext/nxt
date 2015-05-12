package nxt.http;

import nxt.NxtException;
import nxt.PrunableMessage;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllPrunableMessages extends APIServlet.APIRequestHandler {

    static final GetAllPrunableMessages instance = new GetAllPrunableMessages();

    private GetAllPrunableMessages() {
        super(new APITag[] {APITag.MESSAGES}, "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("prunableMessages", jsonArray);

        try (DbIterator<PrunableMessage> messages = PrunableMessage.getAll(firstIndex, lastIndex)) {
            while (messages.hasNext()) {
                jsonArray.add(JSONData.prunableMessage(messages.next(), 0, null));
            }
        }
        return response;
    }

}
