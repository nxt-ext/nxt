package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGoodsCount extends APIServlet.APIRequestHandler {

    static final GetDGSGoodsCount instance = new GetDGSGoodsCount();

    private GetDGSGoodsCount() {
        super(new APITag[] {APITag.DGS}, "seller", "inStockOnly");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String sellerIdValue = Convert.emptyToNull(req.getParameter("seller"));
        long sellerId = sellerIdValue != null ? ParameterParser.getSellerId(req) : 0;
        boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        response.put("numberOfGoods", sellerId != 0
                ? DigitalGoodsStore.Goods.getSellerGoodsCount(sellerId, inStockOnly)
                : inStockOnly ? DigitalGoodsStore.Goods.getCountInStock() : DigitalGoodsStore.Goods.getCount());
        return response;
    }

}
