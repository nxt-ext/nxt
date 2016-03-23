/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DGS_LISTING_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_DGS_LISTING_NAME;
import static nxt.http.JSONResponses.INCORRECT_DGS_LISTING_TAGS;
import static nxt.http.JSONResponses.MISSING_NAME;

public final class DGSListing extends CreateTransaction {

    static final DGSListing instance = new DGSListing();

    private DGSListing() {
        super("messageFile", new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "name", "description", "tags", "quantity", "priceNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String tags = Convert.nullToEmpty(req.getParameter("tags"));
        long priceNQT = ParameterParser.getPriceNQT(req);
        int quantity = ParameterParser.getGoodsQuantity(req);

        if (name == null) {
            return MISSING_NAME;
        }
        name = name.trim();
        if (name.length() > Constants.MAX_DGS_LISTING_NAME_LENGTH) {
            return INCORRECT_DGS_LISTING_NAME;
        }

        if (description.length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH) {
            return INCORRECT_DGS_LISTING_DESCRIPTION;
        }

        if (tags.length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH) {
            return INCORRECT_DGS_LISTING_TAGS;
        }

        //TODO: allow only prunable, binary, image message attachments
        /*
        String messageValue = Convert.emptyToNull(req.getParameter("message"));
        if (messageValue != null) {
            if (!"true".equalsIgnoreCase(req.getParameter("messageIsPrunable"))) {
                return MESSAGE_NOT_PRUNABLE;
            }
            if (!"false".equalsIgnoreCase(req.getParameter("messageIsText"))) {
                return MESSAGE_NOT_BINARY;
            }
            byte[] image = Convert.parseHexString(messageValue);
            Tika tika = new Tika();
            String mediaTypeName = tika.detect(image);
            MediaType mediaType = MediaType.parse(mediaTypeName);
            if (mediaType == null || !mediaType.getType().equals("image")) {
                return MESSAGE_NOT_IMAGE;
            }
        }
        */

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.DigitalGoodsListing(name, description, tags, quantity, priceNQT);
        return createTransaction(req, account, attachment);

    }

    private static final JSONStreamAware MESSAGE_NOT_PRUNABLE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 7);
        response.put("errorDescription", "Only prunable message attachments accepted as DGS listing images");
        MESSAGE_NOT_PRUNABLE = JSON.prepare(response);
    }

    private static final JSONStreamAware MESSAGE_NOT_BINARY;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 8);
        response.put("errorDescription", "Only binary message attachments accepted as DGS listing images");
        MESSAGE_NOT_BINARY = JSON.prepare(response);
    }

    private static final JSONStreamAware MESSAGE_NOT_IMAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 9);
        response.put("errorDescription", "Message attachment is not an image");
        MESSAGE_NOT_IMAGE = JSON.prepare(response);
    }

}
