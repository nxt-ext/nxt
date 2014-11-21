package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSPurchaseCount extends APIServlet.APIRequestHandler {

    static final GetDGSPurchaseCount instance = new GetDGSPurchaseCount();

    private GetDGSPurchaseCount() {
        super(new APITag[] {APITag.DGS}, "seller", "buyer");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long sellerId = ParameterParser.getSellerId(req);
        long buyerId = ParameterParser.getBuyerId(req);

        JSONObject response = new JSONObject();
        if (sellerId != 0 && buyerId == 0) {
            response.put("numberOfPurchases", DigitalGoodsStore.getSellerPurchaseCount(sellerId));
        } else if (sellerId == 0 && buyerId != 0) {
            response.put("numberOfPurchases", DigitalGoodsStore.getBuyerPurchaseCount(buyerId));
        } else if (sellerId == 0 && buyerId == 0) {
            response.put("numberOfPurchases", DigitalGoodsStore.Purchase.getCount());
        } else {
            response.put("errorDescription", "Either seller or buyer must be specified, but not both");
            response.put("errorCode", 3);
        }
        return response;
    }

}
