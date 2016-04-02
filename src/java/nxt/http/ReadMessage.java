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
import nxt.Appendix;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.NO_MESSAGE;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class ReadMessage extends APIServlet.APIRequestHandler {

    static final ReadMessage instance = new ReadMessage();

    private ReadMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }

        Transaction transaction;
        try {
            transaction = Nxt.getBlockchain().getTransaction(Convert.parseUnsignedLong(transactionIdString));
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        JSONObject response = new JSONObject();
        Appendix.Message message = transaction.getMessage();
        Appendix.EncryptedMessage encryptedMessage = transaction.getEncryptedMessage();
        Appendix.EncryptToSelfMessage encryptToSelfMessage = transaction.getEncryptToSelfMessage();
        Appendix.PrunablePlainMessage prunableMessage = transaction.getPrunablePlainMessage();
        Appendix.PrunableEncryptedMessage prunableEncryptedMessage = transaction.getPrunableEncryptedMessage();
        if (message == null && encryptedMessage == null && encryptToSelfMessage == null && prunableMessage == null && prunableEncryptedMessage == null) {
            return NO_MESSAGE;
        }
        if (message != null) {
            response.put("message", Convert.toString(message.getMessage(), message.isText()));
            response.put("messageIsPrunable", false);
        } else if (prunableMessage != null) {
            response.put("message", Convert.toString(prunableMessage.getMessage(), prunableMessage.isText()));
            response.put("messageIsPrunable", true);
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        if (secretPhrase != null) {
            EncryptedData encryptedData = null;
            boolean isText = false;
            boolean uncompress = true;
            if (encryptedMessage != null) {
                encryptedData = encryptedMessage.getEncryptedData();
                isText = encryptedMessage.isText();
                uncompress = encryptedMessage.isCompressed();
                response.put("encryptedMessageIsPrunable", false);
            } else if (prunableEncryptedMessage != null) {
                encryptedData = prunableEncryptedMessage.getEncryptedData();
                isText = prunableEncryptedMessage.isText();
                uncompress = prunableEncryptedMessage.isCompressed();
                response.put("encryptedMessageIsPrunable", true);
            }
            if (encryptedData != null) {
                byte[] readerPublicKey = Crypto.getPublicKey(secretPhrase);
                byte[] senderPublicKey = Account.getPublicKey(transaction.getSenderId());
                byte[] recipientPublicKey = Account.getPublicKey(transaction.getRecipientId());
                byte[] publicKey = Arrays.equals(senderPublicKey, readerPublicKey) ? recipientPublicKey : senderPublicKey;
                if (publicKey != null) {
                    try {
                        byte[] decrypted = Account.decryptFrom(publicKey, encryptedData, secretPhrase, uncompress);
                        response.put("decryptedMessage", Convert.toString(decrypted, isText));
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Decryption of message to recipient failed: " + e.toString());
                        JSONData.putException(response, e, "Wrong secretPhrase");
                    }
                }
            }
            if (encryptToSelfMessage != null) {
                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                try {
                    byte[] decrypted = Account.decryptFrom(publicKey, encryptToSelfMessage.getEncryptedData(), secretPhrase, encryptToSelfMessage.isCompressed());
                    response.put("decryptedMessageToSelf", Convert.toString(decrypted, encryptToSelfMessage.isText()));
                } catch (RuntimeException e) {
                    Logger.logDebugMessage("Decryption of message to self failed: " + e.toString());
                }
            }
        }
        return response;
    }

}
