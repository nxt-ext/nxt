package nxt.http;

import nxt.NxtException;
import nxt.TaggedData;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTaggedData extends APIServlet.APIRequestHandler {

    static final GetTaggedData instance = new GetTaggedData();

    private GetTaggedData() {
        super(new APITag[] {APITag.DATA}, "transaction", "includeData");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        boolean includeData = !"false".equalsIgnoreCase(req.getParameter("includeData"));

        TaggedData taggedData = TaggedData.getData(transactionId);
        if (taggedData != null) {
            return JSONData.taggedData(taggedData, includeData);
        }
        return JSON.emptyJSON;
    }

}
