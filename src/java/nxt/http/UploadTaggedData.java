package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_DATA;
import static nxt.http.JSONResponses.INCORRECT_TAGGED_DATA_DESCRIPTION;
import static nxt.http.JSONResponses.INCORRECT_TAGGED_DATA_FILENAME;
import static nxt.http.JSONResponses.INCORRECT_TAGGED_DATA_NAME;
import static nxt.http.JSONResponses.INCORRECT_TAGGED_DATA_TAGS;
import static nxt.http.JSONResponses.INCORRECT_TAGGED_DATA_TYPE;
import static nxt.http.JSONResponses.MISSING_NAME;

public final class UploadTaggedData extends CreateTransaction {

    static final UploadTaggedData instance = new UploadTaggedData();

    private UploadTaggedData() {
        super(new APITag[] {APITag.DATA, APITag.CREATE_TRANSACTION},
                "name", "description", "tags", "type", "isText", "filename", "data");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));
        String tags = Convert.nullToEmpty(req.getParameter("tags"));
        String type = Convert.nullToEmpty(req.getParameter("type"));
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("isText"));
        String filename = Convert.nullToEmpty(req.getParameter("filename"));
        byte[] data = Convert.parseHexString(Convert.emptyToNull(req.getParameter("data")));


        if (name == null) {
            return MISSING_NAME;
        }
        name = name.trim();
        if (name.length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
            return INCORRECT_TAGGED_DATA_NAME;
        }

        if (description.length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
            return INCORRECT_TAGGED_DATA_DESCRIPTION;
        }

        if (tags.length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
            return INCORRECT_TAGGED_DATA_TAGS;
        }

        if (type.length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
            return INCORRECT_TAGGED_DATA_TYPE;
        }

        if (data == null || data.length == 0 || data.length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
            return INCORRECT_DATA;
        }

        if (filename.length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
            return INCORRECT_TAGGED_DATA_FILENAME;
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.TaggedDataUpload(name, description, tags, type, isText, filename, data);
        return createTransaction(req, account, attachment);

    }

}
