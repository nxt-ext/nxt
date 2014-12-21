package nxt.http;

import nxt.DigitalGoodsStore;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSTagCount extends APIServlet.APIRequestHandler {

    static final GetDGSTagCount instance = new GetDGSTagCount();

    private GetDGSTagCount() {
        super(new APITag[] {APITag.DGS}, "inStockOnly");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final boolean inStockOnly = !"false".equalsIgnoreCase(req.getParameter("inStockOnly"));

        JSONObject response = new JSONObject();
        response.put("numberOfTags", inStockOnly ? DigitalGoodsStore.Tag.getCountInStock() : DigitalGoodsStore.Tag.getCount());
        return response;
    }

}
