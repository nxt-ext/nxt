package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DGS_LISTING_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_DGS_LISTING_NAME;
import static nxt.http.JSONResponses.INCORRECT_DGS_LISTING_TAGS;
import static nxt.http.JSONResponses.INCORRECT_QUANTITY;
import static nxt.http.JSONResponses.MISSING_NAME;

public final class DGSListing extends CreateTransaction {

    static final DGSListing instance = new DGSListing();

    private DGSListing() {
        super("name", "description", "tags", "quantity", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.emptyToNull(req.getParameter("description"));
        String tags = Convert.emptyToNull(req.getParameter("tags"));
        String quantityString = Convert.emptyToNull(req.getParameter("quantity"));
        long priceNQT = ParameterParser.getPriceNQT(req);

        if (name == null) {
            return MISSING_NAME;
        }
        name = name.trim();
        if (name.length() > Constants.MAX_DGS_LISTING_NAME_LENGTH || name.length() == 0) {
            return INCORRECT_DGS_LISTING_NAME;
        }

        if (description != null && description.length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH) {
            return INCORRECT_DGS_LISTING_DESCRIPTION;
        }

        if (tags != null && tags.length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH) {
            return INCORRECT_DGS_LISTING_TAGS;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityString);
            if (quantity < 0 || quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
                return INCORRECT_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.DigitalGoodsListing(name, description, tags, quantity, priceNQT);
        return createTransaction(req, account, attachment);

    }

}
