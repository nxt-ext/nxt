package nxt.http;

import nxt.NxtException;
import nxt.TaggedData;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTaggedData extends APIServlet.APIRequestHandler {

    static final GetTaggedData instance = new GetTaggedData();

    private GetTaggedData() {
        super(new APITag[] {APITag.DATA}, "transaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        TaggedData taggedData = TaggedData.getData(transactionId);
        if (taggedData != null) {
            return JSONData.taggedData(taggedData);
        }
        return JSON.emptyJSON;
    }

}
