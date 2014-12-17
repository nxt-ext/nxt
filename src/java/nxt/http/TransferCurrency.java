package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_CURRENCY;

public final class TransferCurrency extends CreateTransaction {

    static final TransferCurrency instance = new TransferCurrency();

    private TransferCurrency() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "recipient", "currency", "units");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long recipient = ParameterParser.getRecipientId(req);

        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        if (units > account.getUnconfirmedCurrencyUnits(currency.getId())) {
            return NOT_ENOUGH_CURRENCY;
        }

        Attachment attachment = new Attachment.MonetarySystemCurrencyTransfer(currency.getId(), units);
        return createTransaction(req, account, recipient, 0, attachment);
    }

}
