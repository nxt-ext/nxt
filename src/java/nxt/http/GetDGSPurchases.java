package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public final class GetDGSPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSPurchases instance = new GetDGSPurchases();

    private GetDGSPurchases() {
        super("seller", "buyer", "timestamp");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        int timestamp = ParameterParser.getTimestamp(req);
        Long sellerId = ParameterParser.getSellerId(req);
        Long buyerId = ParameterParser.getBuyerId(req);

        Collection<DigitalGoodsStore.Purchase> purchases;
        if (sellerId == null && buyerId == null) {
            purchases = DigitalGoodsStore.getAllPurchases();
        } else if (sellerId != null && buyerId == null) {
            purchases = DigitalGoodsStore.getSellerPurchases(sellerId);
        } else if (sellerId == null) {
            purchases = DigitalGoodsStore.getBuyerPurchases(buyerId);
        } else {
            purchases = DigitalGoodsStore.getSellerBuyerPurchases(sellerId, buyerId);
        }
        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        for (DigitalGoodsStore.Purchase purchase : purchases) {
            if (purchase.getTimestamp() >= timestamp) {
                purchasesJSON.add(JSONData.purchase(purchase));
            }
        }
        response.put("purchases", purchasesJSON);
        return response;
    }

}
