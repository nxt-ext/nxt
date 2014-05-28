package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

import static nxt.http.JSONResponses.MISSING_SELLER;

public final class GetDGSPendingPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSPendingPurchases instance = new GetDGSPendingPurchases();

    private GetDGSPendingPurchases() {
        super("seller");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long sellerId = ParameterParser.getSellerId(req);
        if (sellerId == null) {
            return MISSING_SELLER;
        }

        Collection<DigitalGoodsStore.Purchase> purchases = DigitalGoodsStore.getPendingSellerPurchases(sellerId);
        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        for (DigitalGoodsStore.Purchase purchase : purchases) {
            purchasesJSON.add(JSONData.purchase(purchase));
        }
        response.put("purchases", purchasesJSON);
        return response;
    }

}
