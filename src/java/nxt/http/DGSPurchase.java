package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.crypto.XoredData;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
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

        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted()) {
            return UNKNOWN_GOODS;
        }

        int quantity = ParameterParser.getGoodsQuantity(req);
        if (quantity > goods.getQuantity()) {
            return INCORRECT_PURCHASE_QUANTITY;
        }

        long priceNQT = ParameterParser.getPriceNQT(req);
        if (priceNQT != goods.getPriceNQT()) {
            return INCORRECT_PURCHASE_PRICE;
        }

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

        Attachment attachment = new Attachment.DigitalGoodsPurchase(goods.getId(), quantity, priceNQT, deliveryDeadline, note);
        return createTransaction(req, buyerAccount, attachment);

    }

}
