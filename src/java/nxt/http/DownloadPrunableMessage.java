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
import nxt.Nxt;
import nxt.NxtException;
import nxt.PrunableMessage;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import static nxt.http.JSONResponses.PRUNED_TRANSACTION;

public final class DownloadPrunableMessage extends APIServlet.APIRequestHandler {

    static final DownloadPrunableMessage instance = new DownloadPrunableMessage();

    private DownloadPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase", "retrieve");
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
        String secretPhrase = ParameterParser.getSecretPhrase(request, false);
        byte[] data = null;
        if (prunableMessage != null) {
            if (secretPhrase != null) {
                EncryptedData encryptedData = prunableMessage.getEncryptedData();
                if (encryptedData != null) {
                    byte[] publicKey = prunableMessage.getSenderId() == Account.getId(Crypto.getPublicKey(secretPhrase))
                            ? Account.getPublicKey(prunableMessage.getRecipientId()) : Account.getPublicKey(prunableMessage.getSenderId());
                    if (publicKey != null) {
                        try {
                            data = Account.decryptFrom(publicKey, encryptedData, secretPhrase, prunableMessage.isCompressed());
                        } catch (RuntimeException e) {
                            return JSONResponses.error("Decryption failed");
                        }
                    } else {
                        return JSONResponses.error("Missing public key"); // shouldn't happen
                    }
                }
            } else {
                data = prunableMessage.getMessage();
            }
        }
        if (data == null) {
            data = Convert.EMPTY_BYTE;
        }
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
