package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrency extends APIServlet.APIRequestHandler {

    static final GetCurrency instance = new GetCurrency();

    private GetCurrency() {
        super(new APITag[] {APITag.MS}, "currency", "code", "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));
        return JSONData.currency(ParameterParser.getCurrency(req), includeCounts);
    }

}
