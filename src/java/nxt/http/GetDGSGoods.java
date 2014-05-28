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
        super("seller", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long sellerId = ParameterParser.getSellerId(req);
        int firstIndex, lastIndex;
        try {
            firstIndex = Integer.parseInt(req.getParameter("firstIndex"));
            if (firstIndex < 0) {
                firstIndex = 0;
            }
        } catch (NumberFormatException e) {
            firstIndex = 0;
        }
        try {
            lastIndex = Integer.parseInt(req.getParameter("lastIndex"));
        } catch (NumberFormatException e) {
            lastIndex = Integer.MAX_VALUE;
        }

        JSONObject response = new JSONObject();
        JSONArray goodsJSON = new JSONArray();
        if (sellerId == null) {
            DigitalGoodsStore.Goods[] goods = DigitalGoodsStore.getAllGoods().toArray(new DigitalGoodsStore.Goods[0]);
            for (int i = firstIndex; i <= lastIndex && i < goods.length; i++) {
                goodsJSON.add(JSONData.goods(goods[goods.length - 1 - i]));
            }
        } else {
            Collection<DigitalGoodsStore.Goods> goods = DigitalGoodsStore.getSellerGoods(sellerId);
            for (DigitalGoodsStore.Goods good : goods) {
                goodsJSON.add(JSONData.goods(good));
            }
        }
        response.put("goods", goodsJSON);
        return response;
    }

}
