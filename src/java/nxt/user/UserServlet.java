package nxt.user;

import nxt.Nxt;
import nxt.util.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

import static nxt.user.JSONResponses.DENY_ACCESS;

public final class UserServlet extends HttpServlet  {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        User user = null;

        try {

            String userPasscode = req.getParameter("user");
            if (userPasscode == null) {
                return;
            }

            user = User.getUser(userPasscode);

            if (Nxt.allowedUserHosts != null && !Nxt.allowedUserHosts.contains(req.getRemoteHost())) {
                user.enqueue(DENY_ACCESS);
            } else {
                UserRequestHandler.process(req, user);
            }

        } catch (Exception e) {
            if (user != null) {
                Logger.logMessage("Error processing GET request", e);
            } else {
                Logger.logDebugMessage("Error processing GET request", e);
            }
        }

        if (user != null) {
            user.processPendingResponses(req, resp);
        }

    }

}
