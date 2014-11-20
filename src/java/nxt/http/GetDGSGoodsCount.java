package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
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
        long sellerId = ParameterParser.getSellerId(req);
        boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        response.put("numberOfGoods", DigitalGoodsStore.getSellerGoodsCount(sellerId, inStockOnly));
        return response;
    }

}
