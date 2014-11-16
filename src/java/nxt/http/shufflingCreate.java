package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingCreate extends CreateTransaction {

    static final ShufflingCreate instance = new ShufflingCreate();

    private ShufflingCreate() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
                "currency", "units", "amountNQT", "participantCount", "cancellationHeight");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long currencyId = 0;
        long amount;
        Currency currency = ParameterParser.getCurrency(req, false);
        if (currency != null) {
            currencyId = currency.getId();
            amount = ParameterParser.getLong(req, "units", 1L, Constants.MAX_BALANCE_NQT, true);
        } else {
            amount = ParameterParser.getAmountNQT(req);
        }
        byte participantCount = ParameterParser.getByte(req, "participantCount", Constants.MIN_SHUFFLING_PARTICIPANTS, Constants.MAX_SHUFFLING_PARTICIPANTS);
        int cancellationHeight = ParameterParser.getInt(req, "cancellationHeight", 0, Integer.MAX_VALUE, true);
        Attachment attachment = new Attachment.MonetarySystemShufflingCreation(currencyId, amount, participantCount, cancellationHeight);

        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, attachment);
    }
}
