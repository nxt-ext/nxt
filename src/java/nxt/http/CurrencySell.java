package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class CurrencySell extends CreateTransaction {

    static final CurrencySell instance = new CurrencySell();

    private CurrencySell() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "rateNQT", "units");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        long rateNQT = ParameterParser.getLong(req, "rateNQT", 0, Long.MAX_VALUE, true);
        long units = ParameterParser.getLong(req, "units", Long.MIN_VALUE, Long.MAX_VALUE, true);
        // selling currency for NXT is like exchanging negative number of units
        units = -units;
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MonetarySystemExchange(currency.getId(), rateNQT, units);
        return createTransaction(req, account, attachment);
    }

}
