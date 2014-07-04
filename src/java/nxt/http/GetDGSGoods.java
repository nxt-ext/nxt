package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetDGSGoods extends APIServlet.APIRequestHandler {

    static final GetDGSGoods instance = new GetDGSGoods();

    private GetDGSGoods() {
        super(new APITag[] {APITag.DGS}, "seller", "firstIndex", "lastIndex", "inStockOnly");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long sellerId = ParameterParser.getSellerId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        JSONArray goodsJSON = new JSONArray();
        response.put("goods", goodsJSON);

        List<DigitalGoodsStore.Goods> goods;
        if (sellerId == null) {
            if (inStockOnly) {
                goods = DigitalGoodsStore.getGoodsInStock();
            } else {
                goods = DigitalGoodsStore.getAllGoods();
            }
        } else {
            goods = DigitalGoodsStore.getSellerGoods(sellerId, inStockOnly);
        }

        int count = 0;
        for (DigitalGoodsStore.Goods good : goods) {
            if (count > lastIndex) {
                break;
            }
            if (count >= firstIndex) {
                goodsJSON.add(JSONData.goods(good));
            }
            count++;
        }
        return response;
    }

}
