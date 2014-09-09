package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrency extends APIServlet.APIRequestHandler {

    static final GetCurrency instance = new GetCurrency();

    private GetCurrency() {
        super(new APITag[] {APITag.MS}, "currency");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        return JSONData.currency(ParameterParser.getCurrency(req));
    }

}
