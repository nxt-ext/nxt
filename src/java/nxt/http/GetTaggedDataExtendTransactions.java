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

import nxt.Appendix;
import nxt.Attachment;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.NxtException;
import nxt.TaggedData;
import nxt.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetTaggedDataExtendTransactions extends APIServlet.APIRequestHandler {

    static final GetTaggedDataExtendTransactions instance = new GetTaggedDataExtendTransactions();

    private GetTaggedDataExtendTransactions() {
        super(new APITag[] {APITag.DATA}, "transaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long taggedDataId = ParameterParser.getUnsignedLong(req, "transaction", true);
        List<Long> extendTransactions = TaggedData.getExtendTransactionIds(taggedDataId);
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        Blockchain blockchain = Nxt.getBlockchain();
        Filter<Appendix> filter = (appendix) -> ! (appendix instanceof Attachment.TaggedDataExtend);
        extendTransactions.forEach(transactionId -> jsonArray.add(JSONData.transaction(blockchain.getTransaction(transactionId), filter)));
        response.put("extendTransactions", jsonArray);
        return response;
    }

}
