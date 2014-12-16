package nxt.http;

import nxt.Account;
import nxt.Currency;
import nxt.CurrencyTransfer;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetCurrencyTransfers extends APIServlet.APIRequestHandler {

    static final GetCurrencyTransfers instance = new GetCurrencyTransfers();

    private GetCurrencyTransfers() {
        super(new APITag[] {APITag.MS}, "currency", "account", "firstIndex", "lastIndex", "includeCurrencyInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String currencyId = Convert.emptyToNull(req.getParameter("currency"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));
        boolean includeCurrencyInfo = !"false".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        DbIterator<CurrencyTransfer> transfers = null;
        try {
            if (accountId == null) {
                Currency currency = ParameterParser.getCurrency(req);
                transfers = currency.getTransfers(firstIndex, lastIndex);
            } else if (currencyId == null) {
                Account account = ParameterParser.getAccount(req);
                transfers = account.getCurrencyTransfers(firstIndex, lastIndex);
            } else {
                Currency currency = ParameterParser.getCurrency(req);
                Account account = ParameterParser.getAccount(req);
                transfers = CurrencyTransfer.getAccountCurrencyTransfers(account.getId(), currency.getId(), firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                transfersData.add(JSONData.currencyTransfer(transfers.next(), includeCurrencyInfo));
            }
        } finally {
            DbUtils.close(transfers);
        }
        response.put("transfers", transfersData);

        return response;
    }

    @Override
    boolean startDbTransaction() {
        return true;
    }

}
