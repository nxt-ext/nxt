package nxt.http;

import nxt.Currency;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_CURRENCY;
import static nxt.http.JSONResponses.UNKNOWN_CURRENCY;

public final class GetCurrency extends APIServlet.APIRequestHandler {

    static final GetCurrency instance = new GetCurrency();

    private GetCurrency() {
        super(new APITag[] {APITag.MS}, "currency", "code", "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));
        String currencyValue = Convert.emptyToNull(req.getParameter("currency"));
        Currency currency;
        if (currencyValue == null) {
            String currencyCode = Convert.emptyToNull(req.getParameter("code"));
            if (currencyCode == null) {
                return MISSING_CURRENCY;
            }
            currency = Currency.getCurrencyByCode(currencyCode);
        } else {
            currency = ParameterParser.getCurrency(req);
        }
        if (currency == null) {
            throw new ParameterException(UNKNOWN_CURRENCY);
        }
        return JSONData.currency(currency, includeCounts);
    }

}
