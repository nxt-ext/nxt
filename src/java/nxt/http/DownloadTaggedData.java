package nxt.http;

import nxt.NxtException;
import nxt.TaggedData;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public final class DownloadTaggedData extends APIServlet.APIRequestHandler {

    static final DownloadTaggedData instance = new DownloadTaggedData();

    private DownloadTaggedData() {
        super(new APITag[] {APITag.DATA}, "transaction");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws NxtException  {
        long transactionId = ParameterParser.getUnsignedLong(request, "transaction", true);
        TaggedData taggedData = TaggedData.getData(transactionId);
        byte[] data = taggedData.getData();
        if (!taggedData.getType().equals("")) {
            response.setContentType(taggedData.getType());
        } else {
            response.setContentType("application/octet-stream");
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + taggedData.getFilename());
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
