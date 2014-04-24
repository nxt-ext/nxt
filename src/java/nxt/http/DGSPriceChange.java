package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.UNKNOWN_GOODS;

public final class DGSPriceChange extends CreateTransaction {

    static final DGSPriceChange instance = new DGSPriceChange();

    private DGSPriceChange() {
        super("goods", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getSenderAccount(req);
        Long goodsId = ParameterParser.getGoodsId(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        DigitalGoodsStore.Goods goods = DigitalGoodsStore.getGoods(goodsId);
        if (goods == null || goods.isDelisted() || ! goods.getSellerId().equals(account.getId())) {
            return UNKNOWN_GOODS;
        }
        Attachment attachment = new Attachment.DigitalGoodsPriceChange(goodsId, priceNQT);
        return createTransaction(req, account, attachment);

    }

}
