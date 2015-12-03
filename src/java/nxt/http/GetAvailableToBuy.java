/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
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

import nxt.CurrencyExchangeOffer;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAvailableToBuy extends APIServlet.APIRequestHandler {

    static final GetAvailableToBuy instance = new GetAvailableToBuy();

    private GetAvailableToBuy() {
        super(new APITag[] {APITag.MS}, "currency", "amountNQT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", true);
        long amountNQT = ParameterParser.getAmountNQT(req);
        CurrencyExchangeOffer.AvailableOffers availableOffers = CurrencyExchangeOffer.getAvailableToBuy(currencyId, amountNQT);
        return JSONData.availableOffers(availableOffers);
    }

}
