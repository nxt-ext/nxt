package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.crypto.XoredData;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
import static nxt.http.JSONResponses.INCORRECT_DGS_NOTE;
import static nxt.http.JSONResponses.INCORRECT_DGS_NOTE_NONCE;
import static nxt.http.JSONResponses.INCORRECT_PURCHASE_PRICE;
import static nxt.http.JSONResponses.INCORRECT_PURCHASE_QUANTITY;
import static nxt.http.JSONResponses.MISSING_DELIVERY_DEADLINE_TIMESTAMP;
import static nxt.http.JSONResponses.UNKNOWN_GOODS;

public final class DGSPurchase extends CreateTransaction {

    static final DGSPurchase instance = new DGSPurchase();

    private DGSPurchase() {
        super("goods", "priceNQT", "quantity", "deliveryDeadlineTimestamp",
                "note", "noteNonce");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long goodsId = ParameterParser.getGoodsId(req);
        int quantity = ParameterParser.getGoodsQuantity(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        XoredData note = ParameterParser.getNote(req);
        Account buyerAccount = ParameterParser.getSenderAccount(req);

        String deliveryDeadlineString = Convert.emptyToNull(req.getParameter("deliveryDeadlineTimestamp"));
        if (deliveryDeadlineString == null) {
            return MISSING_DELIVERY_DEADLINE_TIMESTAMP;
        }
        int deliveryDeadline;
        try {
            deliveryDeadline = Integer.parseInt(deliveryDeadlineString);
            if (deliveryDeadline <= Convert.getEpochTime()) {
                return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
        }

        DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(goodsId);
        if (goods == null || goods.isDelisted()) {
            return UNKNOWN_GOODS;
        }
        if (quantity > goods.getQuantity()) {
            return INCORRECT_PURCHASE_QUANTITY;
        }
        if (priceNQT != goods.getPriceNQT()) {
            return INCORRECT_PURCHASE_PRICE;
        }

        Attachment attachment = new Attachment.DigitalGoodsPurchase(goodsId, quantity, priceNQT, deliveryDeadline, note);
        return createTransaction(req, buyerAccount, attachment);

    }

}
