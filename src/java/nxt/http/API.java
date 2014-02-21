package nxt.http;

import nxt.Nxt;
import nxt.util.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class API {

    private static final int DEFAULT_API_PORT = 7876;

    static final Set<String> allowedBotHosts;

    static {
        String allowedBotHostsString = Nxt.getStringProperty("nxt.allowedBotHosts", "127.0.0.1; localhost; 0:0:0:0:0:0:0:1;");
        if (! allowedBotHostsString.equals("*")) {
            Set<String> hosts = new HashSet<>();
            for (String allowedBotHost : allowedBotHostsString.split(";")) {
                allowedBotHost = allowedBotHost.trim();
                if (allowedBotHost.length() > 0) {
                    hosts.add(allowedBotHost);
                }
            }
            allowedBotHosts = Collections.unmodifiableSet(hosts);
        } else {
            allowedBotHosts = null;
        }

        boolean enableAPIServer = Nxt.getBooleanProperty("nxt.enableAPIServer", allowedBotHosts == null || ! allowedBotHosts.isEmpty());
        if (enableAPIServer) {
            try {
                int port = Nxt.getIntProperty("nxt.apiServerPort", API.DEFAULT_API_PORT);
                Server apiServer = new Server(port);
                ServletHandler apiHandler = new ServletHandler();
                apiHandler.addServletWithMapping(APIServlet.class, "/nxt");

                ResourceHandler apiFileHandler = new ResourceHandler();
                apiFileHandler.setDirectoriesListed(true);
                apiFileHandler.setWelcomeFiles(new String[]{"index.html"});
                apiFileHandler.setResourceBase(Nxt.getStringProperty("nxt.apiResourceBase", "html/tools"));

                HandlerList apiHandlers = new HandlerList();
                apiHandlers.setHandlers(new Handler[] { apiFileHandler, apiHandler, new DefaultHandler() });

                apiServer.setHandler(apiHandlers);
                apiServer.setStopAtShutdown(true);
                apiServer.start();
                Logger.logMessage("Started API server on port " + port);
            } catch (Exception e) {
                Logger.logDebugMessage("Failed to start API server", e);
                throw new RuntimeException(e.toString(), e);
            }
        } else {
            Logger.logMessage("API server not enabled");
        }

    }

    public static void init() {}

    private API() {} // never

}
