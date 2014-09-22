package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class CurrencyReserveIncrease extends CreateTransaction {

    static final CurrencyReserveIncrease instance = new CurrencyReserveIncrease();

    private CurrencyReserveIncrease() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "amountNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        long amountNQT = ParameterParser.getAmountNQT(req);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemReserveIncrease(currency.getId(), amountNQT);
        return createTransaction(req, account, attachment);

    }

}
