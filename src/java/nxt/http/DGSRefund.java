package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DGS_REFUND;
import static nxt.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSRefund extends CreateTransaction {

    static final DGSRefund instance = new DGSRefund();

    private DGSRefund() {
        super("purchase", "refundNQT", "note", "noteNonce");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (! sellerAccount.getId().equals(purchase.getSellerId())) {
            return INCORRECT_PURCHASE;
        }

        String refundValueNQT = Convert.emptyToNull(req.getParameter("refundNQT"));
        long refundNQT = 0;
        try {
            if (refundValueNQT != null) {
                refundNQT = Long.parseLong(refundValueNQT);
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_REFUND;
        }
        if (refundNQT < 0 || refundNQT > Constants.MAX_BALANCE_NQT) {
            return INCORRECT_DGS_REFUND;
        }

        String secretPhrase = ParameterParser.getSecretPhrase(req);
        Account buyerAccount = Account.getAccount(purchase.getBuyerId());
        byte[] note = ParameterParser.getNote(req);
        EncryptedData encryptedNote = buyerAccount.encryptTo(note, secretPhrase);

        Attachment attachment = new Attachment.DigitalGoodsRefund(purchase.getId(), refundNQT, encryptedNote);
        return createTransaction(req, sellerAccount, attachment);

    }

}
