package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ShufflingCreate extends CreateTransaction {

    static final ShufflingCreate instance = new ShufflingCreate();

    private ShufflingCreate() {
        super(new APITag[] {APITag.SHUFFLING, APITag.CREATE_TRANSACTION},
                "isCurrency", "currency", "amountNQT", "participantCount", "cancellationHeight");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean isCurrency = ParameterParser.getByte(req, "isCurrency", (byte)0, (byte)1) == 1;
        long currencyId = 0;
        if (isCurrency) {
            currencyId = ParameterParser.getCurrency(req).getId();
        }
        // TODO The amount/units is a weakness, in NXT its specified in NQT but for currency its specified in units
        long amountNQT = ParameterParser.getAmountNQT(req);
        byte participantCount = ParameterParser.getByte(req, "participantCount", Constants.MIN_SHUFFLING_PARTICIPANTS, Constants.MAX_SHUFFLING_PARTICIPANTS);
        int cancellationHeight = ParameterParser.getInt(req, "cancellationHeight", 0, Integer.MAX_VALUE, true);
        Attachment attachment = new Attachment.MonetarySystemShufflingCreation(isCurrency, currencyId, amountNQT, participantCount, cancellationHeight);

        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, attachment);
    }
}
