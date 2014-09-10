package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class CurrencyMint extends CreateTransaction {

    static final CurrencyMint instance = new CurrencyMint();

    private CurrencyMint() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "nonce", "units", "counter");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        long nonce = ParameterParser.getLong(req, "nonce", 0, Long.MAX_VALUE, true);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        long counter = ParameterParser.getLong(req, "counter", 0, Integer.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MonetarySystemMoneyMinting(nonce, currency.getId(), units, counter);
        return createTransaction(req, account, attachment);
    }

}
