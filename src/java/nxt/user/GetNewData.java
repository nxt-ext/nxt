package nxt.user;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

final class GetNewData extends UserRequestHandler {

    static final GetNewData instance = new GetNewData();

    private GetNewData() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        return null;
    }
}
