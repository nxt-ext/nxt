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
        PhasingPoll poll = ParameterParser.getPhasingPoll(req);
        boolean countVotes = ParameterParser.getBoolean(req, "countVotes", false);
        boolean includeVoters = ParameterParser.getBoolean(req, "includeVoters", false);
        return JSONData.phasingPoll(poll, countVotes, includeVoters);
    }
}