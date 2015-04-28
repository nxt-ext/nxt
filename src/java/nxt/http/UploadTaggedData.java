package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class UploadTaggedData extends CreateTransaction {

    static final UploadTaggedData instance = new UploadTaggedData();

    private UploadTaggedData() {
        super(new APITag[] {APITag.DATA, APITag.CREATE_TRANSACTION},
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getSenderAccount(req);
        Attachment.TaggedDataUpload taggedDataUpload = ParameterParser.getTaggedData(req);
        return createTransaction(req, account, taggedDataUpload);

    }

}
