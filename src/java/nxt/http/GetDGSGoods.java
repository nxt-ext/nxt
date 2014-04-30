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
        super("seller");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long sellerId = ParameterParser.getSellerId(req);
        JSONObject response = new JSONObject();
        JSONArray goodsJSON = new JSONArray();
        Collection<DigitalGoodsStore.Goods> goods = sellerId == null ?
                DigitalGoodsStore.getAllGoods() : DigitalGoodsStore.getSellerGoods(sellerId);
        for (DigitalGoodsStore.Goods good : goods) {
            goodsJSON.add(JSONData.goods(good));
        }
        response.put("goods", goodsJSON);
        return response;
    }

}
