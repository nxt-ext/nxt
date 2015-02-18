package nxt.http;

import nxt.NxtException;
import nxt.PhasingPoll;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPhasingPoll extends APIServlet.APIRequestHandler {

    static final GetPhasingPoll instance = new GetPhasingPoll();

    private GetPhasingPoll() {
        super(new APITag[]{APITag.PHASING}, "transaction", "countVotes", "includeVoters");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        boolean countVotes = ParameterParser.getBoolean(req, "countVotes", false);
        boolean includeVoters = ParameterParser.getBoolean(req, "includeVoters", false);
        PhasingPoll phasingPoll = PhasingPoll.getPoll(transactionId);
        if (phasingPoll != null) {
            return JSONData.phasingPoll(phasingPoll, countVotes, includeVoters);
        }
        PhasingPoll.PhasingPollResult pollResult = PhasingPoll.getResult(transactionId);
        if (pollResult != null) {
            return JSONData.phasingPollResult(pollResult);
        }
        return JSONResponses.UNKNOWN_TRANSACTION;
    }
}