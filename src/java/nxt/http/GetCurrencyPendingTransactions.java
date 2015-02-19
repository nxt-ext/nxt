package nxt.http;

import nxt.Currency;
import nxt.PhasingPoll;
import nxt.Transaction;
import nxt.VoteWeighting;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetCurrencyPendingTransactions extends APIServlet.APIRequestHandler {
    static final GetCurrencyPendingTransactions instance = new GetCurrencyPendingTransactions();

    private GetCurrencyPendingTransactions() {
        super(new APITag[]{APITag.AE, APITag.PHASING}, "currency", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Currency currency = ParameterParser.getCurrency(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        long currencyId = currency.getId();

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator =
                     PhasingPoll.getHoldingPendingTransactions(currencyId, VoteWeighting.VotingModel.CURRENCY, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction));
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;
    }

}
