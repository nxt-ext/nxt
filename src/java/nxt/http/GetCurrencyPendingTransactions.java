package nxt.http;

import nxt.Constants;
import nxt.Currency;
import nxt.PendingTransactionPoll;
import nxt.Transaction;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetCurrencyPendingTransactions extends APIServlet.APIRequestHandler {
    static final GetCurrencyPendingTransactions instance = new GetCurrencyPendingTransactions();

    private GetCurrencyPendingTransactions() {
        super(new APITag[]{APITag.AE, APITag.PENDING_TRANSACTIONS}, "currency", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Currency currency = ParameterParser.getCurrency(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        long currencyId = currency.getId();
        byte votingModel = Constants.VOTING_MODEL_CURRENCY;

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator =
                     PendingTransactionPoll.getPendingTransactionsForHolding(currencyId, votingModel, firstIndex, lastIndex)) {
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
