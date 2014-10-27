package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetShuffling extends APIServlet.APIRequestHandler {

    static final GetShuffling instance = new GetShuffling();

    private GetShuffling() {
        super(new APITag[] {APITag.SHUFFLING}, "shuffling");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        return JSONData.shuffling(ParameterParser.getShuffling(req));
    }

}
