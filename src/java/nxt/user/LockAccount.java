package nxt.user;

import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

final class LockAccount extends UserRequestHandler {

    static final LockAccount instance = new LockAccount();

    private LockAccount() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req, User user) throws IOException {

        user.deinitializeKeyPair();

        JSONObject response = new JSONObject();
        response.put("response", "lockAccount");

        return response;
    }
}
