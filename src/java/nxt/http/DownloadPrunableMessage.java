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

import nxt.Nxt;
import nxt.NxtException;
import nxt.PrunableMessage;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import static nxt.http.JSONResponses.PRUNED_TRANSACTION;

public final class DownloadPrunableMessage extends APIServlet.APIRequestHandler {

    static final DownloadPrunableMessage instance = new DownloadPrunableMessage();

    private DownloadPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "retrieve");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws NxtException {
        long transactionId = ParameterParser.getUnsignedLong(request, "transaction", true);
        boolean retrieve = "true".equalsIgnoreCase(request.getParameter("retrieve"));
        PrunableMessage prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
        if (prunableMessage == null && retrieve) {
            if (Nxt.getBlockchainProcessor().restorePrunedTransaction(transactionId) == null) {
                return PRUNED_TRANSACTION;
            }
            prunableMessage = PrunableMessage.getPrunableMessage(transactionId);
        }
        if (prunableMessage == null || prunableMessage.getMessage() == null) {
            return JSON.emptyJSON;
        }
        byte[] data = prunableMessage.getMessage();
        response.setHeader("Content-Disposition", "inline; filename=" + Long.toUnsignedString(transactionId));
        response.setContentLength(data.length);
        try (OutputStream out = response.getOutputStream()) {
            try {
                out.write(data);
            } catch (IOException e) {
                throw new ParameterException(JSONResponses.RESPONSE_WRITE_ERROR);
            }
        } catch (IOException e) {
            throw new ParameterException(JSONResponses.RESPONSE_STREAM_ERROR);
        }
        return null;
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        throw new UnsupportedOperationException();
    }
}
