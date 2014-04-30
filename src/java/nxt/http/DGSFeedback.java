package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.crypto.XoredData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSFeedback extends CreateTransaction {

    static final DGSFeedback instance = new DGSFeedback();

    private DGSFeedback() {
        super("purchase", "note", "noteNonce");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        XoredData note = ParameterParser.getNote(req);
        Account buyerAccount = ParameterParser.getSenderAccount(req);
        if (! buyerAccount.getId().equals(purchase.getBuyerId())) {
            return INCORRECT_PURCHASE;
        }

        Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId(), note);
        return createTransaction(req, buyerAccount, attachment);

    }

}
