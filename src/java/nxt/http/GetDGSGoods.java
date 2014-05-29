package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public final class GetDGSGoods extends APIServlet.APIRequestHandler {

    static final GetDGSGoods instance = new GetDGSGoods();

    private GetDGSGoods() {
        super("seller", "firstIndex", "lastIndex", "inStockOnly");
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

        if (sellerId == null) {
            DigitalGoodsStore.Goods[] goods = DigitalGoodsStore.getAllGoods().toArray(new DigitalGoodsStore.Goods[0]);
            for (int i = firstIndex; goodsJSON.size() <= lastIndex - firstIndex + 1 && i < goods.length; i++) {
                DigitalGoodsStore.Goods good = goods[goods.length - 1 - i];
                if (inStockOnly && (((good.isDelisted() || good.getQuantity() == 0)))) {
                    continue;
                }
                goodsJSON.add(JSONData.goods(goods[goods.length - 1 - i]));
            }
            return response;
        }

        Collection<DigitalGoodsStore.Goods> goods = DigitalGoodsStore.getSellerGoods(sellerId);
        int i = 0;
        for (DigitalGoodsStore.Goods good : goods) {
            if (i > lastIndex) {
                break;
            }
            if (i >= firstIndex) {
                goodsJSON.add(JSONData.goods(good));
            }
            i++;
        }
        return response;
    }

}
