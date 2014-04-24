package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DELTA_QUANTITY;
import static nxt.http.JSONResponses.MISSING_DELTA_QUANTITY;
import static nxt.http.JSONResponses.UNKNOWN_GOODS;

public final class DGSQuantityChange extends CreateTransaction {

    static final DGSQuantityChange instance = new DGSQuantityChange();

    private DGSQuantityChange() {
        super("goods", "deltaQuantity");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getSenderAccount(req);
        Long goodsId = ParameterParser.getGoodsId(req);
        int deltaQuantity;
        try {
            String deltaQuantityString = Convert.emptyToNull(req.getParameter("deltaQuantity"));
            if (deltaQuantityString == null) {
                return MISSING_DELTA_QUANTITY;
            }
            deltaQuantity = Integer.parseInt(deltaQuantityString);
            if (deltaQuantity > Constants.MAX_DGS_LISTING_QUANTITY || deltaQuantity < -Constants.MAX_DGS_LISTING_QUANTITY) {
                return INCORRECT_DELTA_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELTA_QUANTITY;
        }
        DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(goodsId);
        if (goods == null || goods.isDelisted() || ! goods.getSellerId().equals(account.getId())) {
            return UNKNOWN_GOODS;
        }
        Attachment attachment = new Attachment.DigitalGoodsQuantityChange(goodsId, deltaQuantity);
        return createTransaction(req, account, attachment);

    }

}
