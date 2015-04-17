package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import nxt.TaggedData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class ExtendTaggedData extends CreateTransaction {

    static final ExtendTaggedData instance = new ExtendTaggedData();

    private ExtendTaggedData() {
        super(new APITag[] {APITag.DATA, APITag.CREATE_TRANSACTION}, "transaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getSenderAccount(req);
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        TaggedData taggedData = TaggedData.getData(transactionId);
        if (taggedData == null) {
            return UNKNOWN_TRANSACTION;
        }
        Attachment.TaggedDataExtend taggedDataExtend = new Attachment.TaggedDataExtend(taggedData);
        return createTransaction(req, account, taggedDataExtend);

    }

}
