package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Increase the value of currency units by paying NXT
 * </p>
 * Parameters
 * <ul>
 * <li>currency - currency id
 * <li>amountNQT - the NXT amount invested into increasing the value of a single currency unit.<br>
 * This value is multiplied by the currency total supply and result is deducted from the sender account balance.
 * </ul>
 * </p>
 * Constraints
 * <This transaction is allowed only in case the currency is {@link nxt.CurrencyType#INFLATABLE} or in case
 * the currency is {@link nxt.CurrencyType#RESERVABLE} and is not yet active.
 * </p>
 * In case the currency is not active yet, the sender account is becoming a founder. Once the currency becomes active
 * the total supply is distributed between the founders based on their proportional investment<br>
 * The current list of founders can be obtained using the {@link nxt.http.GetCurrencyFounders} API.
 */

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
