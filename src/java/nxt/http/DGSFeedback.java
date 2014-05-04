package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSFeedback extends CreateTransaction {

    static final DGSFeedback instance = new DGSFeedback();

    private DGSFeedback() {
        super("purchase", "note");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);

        Account buyerAccount = ParameterParser.getSenderAccount(req);
        if (! buyerAccount.getId().equals(purchase.getBuyerId())) {
            return INCORRECT_PURCHASE;
        }

        String secretPhrase = ParameterParser.getSecretPhrase(req);
        byte[] note = ParameterParser.getNote(req);
        Account sellerAccount = Account.getAccount(purchase.getSellerId());
        EncryptedData encryptedNote = sellerAccount.encryptTo(note, secretPhrase);

        Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId(), encryptedNote);
        return createTransaction(req, buyerAccount, attachment);
    }

}
