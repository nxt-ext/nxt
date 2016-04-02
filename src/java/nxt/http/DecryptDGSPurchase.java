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
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static nxt.http.JSONResponses.DECRYPTION_FAILED;

public final class DecryptDGSPurchase extends APIServlet.APIRequestHandler {

    static final DecryptDGSPurchase instance = new DecryptDGSPurchase();

    private DecryptDGSPurchase() {
        super(new APITag[] {APITag.DGS}, "purchase", "secretPhrase", "sharedKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);

        byte[] sharedKey = ParameterParser.getBytes(req, "sharedKey", false);
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        if (sharedKey.length != 0 && secretPhrase != null) {
            return JSONResponses.either("secretPhrase", "sharedKey");
        }
        if (sharedKey.length == 0 && secretPhrase == null) {
            return JSONResponses.missing("secretPhrase", "sharedKey");
        }
        if (purchase.getEncryptedGoods() == null) {
            return JSONResponses.GOODS_NOT_DELIVERED;
        }
        byte[] data = purchase.getEncryptedGoods().getData();
        try {
            byte[] decrypted = Convert.EMPTY_BYTE;
            if (data.length != 0) {
                if (secretPhrase != null) {
                    byte[] readerPublicKey = Crypto.getPublicKey(secretPhrase);
                    byte[] sellerPublicKey = Account.getPublicKey(purchase.getSellerId());
                    byte[] buyerPublicKey = Account.getPublicKey(purchase.getBuyerId());
                    byte[] publicKey = Arrays.equals(sellerPublicKey, readerPublicKey) ? buyerPublicKey : sellerPublicKey;
                    if (publicKey != null) {
                        decrypted = Account.decryptFrom(publicKey, purchase.getEncryptedGoods(), secretPhrase, true);
                    }
                } else {
                    decrypted = Crypto.aesDecrypt(purchase.getEncryptedGoods().getData(), sharedKey);
                    decrypted = Convert.uncompress(decrypted);
                }
            }
            JSONObject response = new JSONObject();
            response.put("decryptedGoods", Convert.toString(decrypted, purchase.goodsIsText()));
            return response;
        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString());
            return DECRYPTION_FAILED;
        }
    }

}
