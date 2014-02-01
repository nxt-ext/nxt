package nxt.user;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static nxt.user.JSONResponses.LOCK_ACCOUNT;

final class LockAccount extends UserRequestHandler {

    static final LockAccount instance = new LockAccount();

    private LockAccount() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {

        user.deinitializeKeyPair();

        return LOCK_ACCOUNT;
    }
}
