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
import nxt.Asset;
import nxt.Attachment;
import nxt.Constants;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class DeleteAsset extends CreateTransaction {

    static final DeleteAsset instance = new DeleteAsset();

    private DeleteAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);

        long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        JSONStreamAware response;
        if (Nxt.getBlockchain().getHeight() < Constants.ASSET_DELETE_BLOCK) {
            // Use AssetTransfer attachment if asset delete is not enabled yet
            Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset.getId(), quantityQNT);
            response = createTransaction(req, account, Genesis.CREATOR_ID, 0, attachment);
        } else {
            // Use AssetDelete attachment
            Attachment attachment = new Attachment.ColoredCoinsAssetDelete(asset.getId(), quantityQNT);
            response = createTransaction(req, account, attachment);
        }
        return response;
    }

}
