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

import static nxt.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static nxt.http.JSONResponses.INCORRECT_DGS_GOODS;
import static nxt.http.JSONResponses.INCORRECT_PURCHASE;

public final class DGSDelivery extends CreateTransaction {

    static final DGSDelivery instance = new DGSDelivery();

    private DGSDelivery() {
        super("purchase", "discountNQT", "goodsData");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        String secretPhrase = ParameterParser.getSecretPhrase(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (! sellerAccount.getId().equals(purchase.getSellerId())) {
            return INCORRECT_PURCHASE;
        }

        String discountValueNQT = Convert.emptyToNull(req.getParameter("discountNQT"));
        long discountNQT = 0;
        try {
            if (discountValueNQT != null) {
                discountNQT = Long.parseLong(discountValueNQT);
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_DISCOUNT;
        }
        if (discountNQT < 0 || discountNQT > Constants.MAX_BALANCE_NQT) {
            return INCORRECT_DGS_DISCOUNT;
        }

        byte[] goodsData;
        try {
            goodsData = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("goodsData")));
            if (goodsData.length > Constants.MAX_DGS_GOODS_LENGTH) {
                return INCORRECT_DGS_GOODS;
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_GOODS;
        }

        Account buyerAccount = Account.getAccount(purchase.getBuyerId());
        EncryptedData encryptedGoods = buyerAccount.encryptTo(goodsData, secretPhrase);

        Attachment attachment = new Attachment.DigitalGoodsDelivery(purchase.getId(), encryptedGoods, discountNQT);
        return createTransaction(req, sellerAccount, attachment);

    }

}
