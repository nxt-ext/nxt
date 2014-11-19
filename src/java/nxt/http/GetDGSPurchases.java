package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.FilteringIterator;
import nxt.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSPurchases instance = new GetDGSPurchases();

    private GetDGSPurchases() {
        super(new APITag[] {APITag.DGS}, "seller", "buyer", "firstIndex", "lastIndex", "completed", "withPublicFeedbacksOnly");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long sellerId = ParameterParser.getSellerId(req);
        long buyerId = ParameterParser.getBuyerId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final boolean completed = "true".equalsIgnoreCase(req.getParameter("completed"));
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));


        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        response.put("purchases", purchasesJSON);

        if (sellerId == 0 && buyerId == 0) {
            try (FilteringIterator<DigitalGoodsStore.Purchase> purchaseIterator = new FilteringIterator<>(DigitalGoodsStore.getAllPurchases(0, -1),
                    new Filter<DigitalGoodsStore.Purchase>() {
                        @Override
                        public boolean ok(DigitalGoodsStore.Purchase purchase) {
                            return ! (completed && purchase.isPending()) && (! withPublicFeedbacksOnly || purchase.hasPublicFeedbacks());
                        }
                    }, firstIndex, lastIndex)) {
                while (purchaseIterator.hasNext()) {
                    purchasesJSON.add(JSONData.purchase(purchaseIterator.next()));
                }
            }
            return response;
        }

        DbIterator<DigitalGoodsStore.Purchase> purchases;
        if (sellerId != 0 && buyerId == 0) {
            purchases = DigitalGoodsStore.getSellerPurchases(sellerId, 0, -1);
        } else if (sellerId == 0) {
            purchases = DigitalGoodsStore.getBuyerPurchases(buyerId, 0, -1);
        } else {
            purchases = DigitalGoodsStore.getSellerBuyerPurchases(sellerId, buyerId, 0, -1);
        }
        try (FilteringIterator<DigitalGoodsStore.Purchase> purchaseIterator = new FilteringIterator<>(purchases,
                new Filter<DigitalGoodsStore.Purchase>() {
                    @Override
                    public boolean ok(DigitalGoodsStore.Purchase purchase) {
                        return ! (completed && purchase.isPending()) && (! withPublicFeedbacksOnly || purchase.hasPublicFeedbacks());
                    }
                }, firstIndex, lastIndex)) {
            while (purchaseIterator.hasNext()) {
                purchasesJSON.add(JSONData.purchase(purchaseIterator.next()));
            }
        }
        return response;
    }

}
