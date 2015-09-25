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

import nxt.Account;
import nxt.NxtException;
import nxt.Shuffler;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_PUBLIC_KEY;
import static nxt.http.JSONResponses.MISSING_RECIPIENT_SECRET_PHRASE_OR_PUBLIC_KEY;

public final class StartShuffler extends APIServlet.APIRequestHandler {

    static final StartShuffler instance = new StartShuffler();

    private StartShuffler() {
        super(new APITag[]{APITag.SHUFFLING}, "secretPhrase", "shufflingFullHash", "recipientSecretPhrase", "recipientPublicKey");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        byte[] shufflingFullHash = ParameterParser.getBytes(req, "shufflingFullHash", true);
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        String recipientSecretPhrase = Convert.emptyToNull(req.getParameter("recipientSecretPhrase"));
        byte[] recipientPublicKey;
        if (recipientSecretPhrase == null) {
            recipientPublicKey = Convert.parseHexString(Convert.emptyToNull(req.getParameter("recipientPublicKey")));
            if (recipientPublicKey == null) {
                return MISSING_RECIPIENT_SECRET_PHRASE_OR_PUBLIC_KEY;
            }
        } else {
            recipientPublicKey = Crypto.getPublicKey(recipientSecretPhrase);
        }
        if (Account.getAccount(recipientPublicKey) != null) {
            return INCORRECT_PUBLIC_KEY; // do not allow existing account to be used as recipient
        }
        Shuffler shuffler = Shuffler.addOrGetShuffler(secretPhrase, recipientPublicKey, shufflingFullHash);
        return shuffler != null ? JSONData.shuffler(shuffler) : JSON.emptyJSON;
    }

}
