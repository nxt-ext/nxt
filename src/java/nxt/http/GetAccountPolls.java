package nxt.http;


import nxt.Account;
import nxt.NxtException;
import nxt.Poll;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetAccountPolls extends APIServlet.APIRequestHandler {

    static final GetAccountPolls instance = new GetAccountPolls();

    private GetAccountPolls() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.VS}, "includeVoters", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Account account = ParameterParser.getAccount(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeVoters = "true".equalsIgnoreCase(req.getParameter("includeVoters"));

        DbIterator<Poll> polls = Poll.getPollsByAccount(account.getId(), firstIndex, lastIndex);

        JSONArray pollsJson = new JSONArray();
        while (polls.hasNext()) {
            pollsJson.add(JSONData.poll(polls.next(), includeVoters));
        }

        JSONObject response = new JSONObject();
        response.put("polls", pollsJson);
        return response;
    }
}
