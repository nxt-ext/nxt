package nxt.http;

import nxt.NxtException;
import nxt.Poll;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPollIds extends APIServlet.APIRequestHandler {

    static final GetPollIds instance = new GetPollIds();

    private GetPollIds() {
        super(new APITag[]{APITag.VS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String accountValue = Convert.emptyToNull(req.getParameter("account"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        DbIterator<Poll> polls;

        if (accountValue == null) {
            polls = Poll.getAllPolls(firstIndex, lastIndex);
        } else {
            polls = Poll.getPollsByAccount(Convert.parseAccountId(accountValue), firstIndex, lastIndex);
        }

        JSONArray pollIds = new JSONArray();
        while (polls.hasNext()) {
            pollIds.add(Convert.toUnsignedLong(polls.next().getId()));
        }

        JSONObject response = new JSONObject();
        response.put("pollIds", pollIds);
        return response;
    }
}
