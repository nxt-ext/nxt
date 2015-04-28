package nxt.http;

import nxt.PhasingPoll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;

public final class GetPhasingPolls extends APIServlet.APIRequestHandler {

    static final GetPhasingPolls instance = new GetPhasingPolls();

    private GetPhasingPolls() {
        super(new APITag[] {APITag.PHASING}, "transaction", "transaction", "transaction", "countVotes"); // limit to 3 for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String[] transactions = req.getParameterValues("transaction");
        if (transactions == null) {
            return MISSING_TRANSACTION;
        }
        boolean countVotes = "true".equalsIgnoreCase(req.getParameter("countVotes"));

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("polls", jsonArray);
        for (String transactionIdValue : transactions) {
            if (Convert.emptyToNull(transactionIdValue) == null) {
                continue;
            }
            try {
                long transactionId = Convert.parseUnsignedLong(transactionIdValue);
                PhasingPoll poll = PhasingPoll.getPoll(transactionId);
                if (poll != null) {
                    jsonArray.add(JSONData.phasingPoll(poll, countVotes));
                } else {
                    PhasingPoll.PhasingPollResult pollResult = PhasingPoll.getResult(transactionId);
                    if (pollResult != null) {
                        jsonArray.add(JSONData.phasingPollResult(pollResult));
                    }
                }
            } catch (RuntimeException e) {
                return INCORRECT_TRANSACTION;
            }
        }
        return response;
    }

}
