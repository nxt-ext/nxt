package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Reduce the value of currency units and claim back NXT invested into this currency
 * </p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>units - the number of currency units claimed.<br>
 * This value is multiplied by current the currency total supply and the result is added  to the sender NXT account balance.
 * </ul>
 * </p>
 * Constraints
 * <This transaction is allowed only in case the currency is {@link nxt.CurrencyType#INFLATABLE} or in case
 * the currency is {@link nxt.CurrencyType#RESERVABLE} and is not yet active.<br>
 * In case the currency is not active yet, only founders can claim their investment.
 * Once the currency is active
 */
public final class CurrencyReserveClaim extends CreateTransaction {

    static final CurrencyReserveClaim instance = new CurrencyReserveClaim();

    private CurrencyReserveClaim() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "units");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, currency.getTotalSupply(), false);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MonetarySystemReserveClaim(currency.getId(), units);
        return createTransaction(req, account, attachment);

    }

}
