package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBalance extends APIServlet.APIRequestHandler {

    static final GetBalance instance = new GetBalance();

    private GetBalance() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "includeEffectiveBalance");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean includeEffectiveBalance = !"false".equalsIgnoreCase(req.getParameter("includeEffectiveBalance"));
        return JSONData.accountBalance(ParameterParser.getAccount(req), includeEffectiveBalance);
    }

}
