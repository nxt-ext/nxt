package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.db.FilteringIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchDGSGoods extends APIServlet.APIRequestHandler {

    static final SearchDGSGoods instance = new SearchDGSGoods();

    private SearchDGSGoods() {
        super(new APITag[] {APITag.DGS, APITag.SEARCH}, "query", "seller", "firstIndex", "lastIndex", "inStockOnly", "hideDelisted");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long sellerId = ParameterParser.getSellerId(req);
        String query = Convert.nullToEmpty(req.getParameter("query"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));
        boolean hideDelisted = "true".equalsIgnoreCase(req.getParameter("hideDelisted"));

        JSONObject response = new JSONObject();
        JSONArray goodsJSON = new JSONArray();
        response.put("goods", goodsJSON);

        FilteringIterator.Filter<DigitalGoodsStore.Goods> filter = hideDelisted ?
                new FilteringIterator.Filter<DigitalGoodsStore.Goods>() {
                    @Override
                    public boolean ok(DigitalGoodsStore.Goods goods) {
                        return ! goods.isDelisted();
                    }
                } :
                new FilteringIterator.Filter<DigitalGoodsStore.Goods>() {
                    @Override
                    public boolean ok(DigitalGoodsStore.Goods goods) {
                        return true;
                    }
                };

        FilteringIterator<DigitalGoodsStore.Goods> iterator = null;
        try {
            DbIterator<DigitalGoodsStore.Goods> goods;
            if (sellerId == 0) {
                goods = DigitalGoodsStore.searchGoods(query, inStockOnly, 0, -1);
            } else {
                goods = DigitalGoodsStore.searchSellerGoods(query, sellerId, inStockOnly, 0, -1);
            }
            iterator = new FilteringIterator<>(goods, filter, firstIndex, lastIndex);
            while (iterator.hasNext()) {
                DigitalGoodsStore.Goods good = iterator.next();
                goodsJSON.add(JSONData.goods(good));
            }
        } finally {
            DbUtils.close(iterator);
        }

        return response;
    }

}
