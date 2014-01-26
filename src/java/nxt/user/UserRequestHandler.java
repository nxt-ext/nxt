package nxt.user;

import nxt.util.Logger;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class UserRequestHandler {

    private static final Map<String,UserRequestHandler> userRequestHandlers;

    static {

        Map<String,UserRequestHandler> map = new HashMap<>();

        map.put("generateAuthorizationToken", GenerateAuthorizationToken.instance);
        map.put("getInitialData", GetInitialData.instance);
        map.put("getNewData", GetNewData.instance);
        map.put("lockAccount", LockAccount.instance);
        map.put("removeActivePeer", RemoveActivePeer.instance);
        map.put("removeBlacklistedPeer", RemoveBlacklistedPeer.instance);
        map.put("removeKnownPeer", RemoveKnownPeer.instance);
        map.put("sendMoney", SendMoney.instance);
        map.put("unlockAccount", UnlockAccount.instance);

        userRequestHandlers = Collections.unmodifiableMap(map);
    }

    public static void process(HttpServletRequest req, User user) throws ServletException, IOException {

        try {
            String requestType = req.getParameter("requestType");

            if (requestType != null) {
                UserRequestHandler userRequestHandler = userRequestHandlers.get(requestType);
                if (userRequestHandler != null) {
                    JSONObject response = userRequestHandler.processRequest(req, user);
                    if (response != null) {
                        user.enqueue(response);
                    }
                    return;
                }
            }
            JSONObject response = new JSONObject();
            response.put("response", "showMessage");
            response.put("message", "Incorrect request!");
            user.enqueue(response);

        } catch (Exception e) {
            Logger.logMessage("Error processing GET request", e);
            JSONObject response = new JSONObject();
            response.put("response", "showMessage");
            response.put("message", e.toString());
            user.enqueue(response);
        }

    }

    abstract JSONObject processRequest(HttpServletRequest request, User user) throws IOException;

}
