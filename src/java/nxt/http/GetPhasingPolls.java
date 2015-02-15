package nxt.http;

import nxt.PhasingPoll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class GetPhasingPolls extends APIServlet.APIRequestHandler {

    static final GetPhasingPolls instance = new GetPhasingPolls();

    private GetPhasingPolls() {
        super(new APITag[] {APITag.PHASING}, "transaction", "transaction", "transaction", "countVotes", "includeVoters"); // limit to 3 for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String[] transactions = req.getParameterValues("transaction");
        boolean countVotes = ParameterParser.getBoolean(req, "countVotes", false);
        boolean includeVoters = ParameterParser.getBoolean(req, "includeVoters", false);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("polls", jsonArray);
        for (String transactionId : transactions) {
            if (Convert.emptyToNull(transactionId) == null) {
                continue;
            }
            try {
                PhasingPoll poll = PhasingPoll.getPoll(Convert.parseUnsignedLong(transactionId));
                if (poll == null) {
                    return UNKNOWN_TRANSACTION;
                }
                jsonArray.add(JSONData.phasingPoll(poll, countVotes, includeVoters));
            } catch (RuntimeException e) {
                return INCORRECT_TRANSACTION;
            }
        }
        return response;
    }

}
