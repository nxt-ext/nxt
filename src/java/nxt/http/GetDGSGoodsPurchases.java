package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGoodsPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSGoodsPurchases instance = new GetDGSGoodsPurchases();

    private GetDGSGoodsPurchases() {
        super(new APITag[] {APITag.DGS}, "goods", "firstIndex", "lastIndex", "withPublicFeedbacksOnly");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));


        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        response.put("purchases", purchasesJSON);

        try (DbIterator<DigitalGoodsStore.Purchase> iterator = DigitalGoodsStore.getGoodsPurchases(goods.getId(), withPublicFeedbacksOnly, firstIndex, lastIndex)) {
            while(iterator.hasNext()) {
                purchasesJSON.add(JSONData.purchase(iterator.next()));
            }
        }
        return response;
    }

}
