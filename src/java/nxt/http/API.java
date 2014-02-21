package nxt.http;

import nxt.Nxt;
import nxt.util.Logger;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class API {

    static final Set<String> allowedBotHosts;

    static {
        String allowedBotHostsString = Nxt.getStringProperty("nxt.allowedBotHosts");
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

        boolean enableAPIServer = Nxt.getBooleanProperty("nxt.enableAPIServer");
        if (enableAPIServer) {
            try {
                int port = Nxt.getIntProperty("nxt.apiServerPort");
                String host = Nxt.getStringProperty("nxt.apiServerHost");
                Server apiServer = new Server();
                ServerConnector connector;

                boolean enableSSL = Nxt.getBooleanProperty("nxt.apiSSL");
                if (enableSSL) {
                    Logger.logMessage("Using SSL (https) for the API server");
                    HttpConfiguration https_config = new HttpConfiguration();
                    https_config.setSecureScheme("https");
                    https_config.setSecurePort(port);
                    https_config.addCustomizer(new SecureRequestCustomizer());
                    SslContextFactory sslContextFactory = new SslContextFactory();
                    sslContextFactory.setKeyStorePath(Nxt.getStringProperty("nxt.keyStorePath"));
                    sslContextFactory.setKeyStorePassword(Nxt.getStringProperty("nxt.keyStorePassword"));
                    sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                            "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                    connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                            new HttpConnectionFactory(https_config));
                } else {
                    connector = new ServerConnector(apiServer);
                }

                connector.setPort(port);
                connector.setHost(host);
                connector.setIdleTimeout(Nxt.getIntProperty("nxt.apiServerIdleTimeout"));
                apiServer.addConnector(connector);

                HandlerList apiHandlers = new HandlerList();

                String apiResourceBase = Nxt.getStringProperty("nxt.apiResourceBase");
                if (apiResourceBase != null) {
                    ResourceHandler apiFileHandler = new ResourceHandler();
                    apiFileHandler.setDirectoriesListed(true);
                    apiFileHandler.setWelcomeFiles(new String[]{"index.html"});
                    apiFileHandler.setResourceBase(apiResourceBase);
                    apiHandlers.addHandler(apiFileHandler);
                }

                String javadocResourceBase = Nxt.getStringProperty("nxt.javadocResourceBase");
                if (javadocResourceBase != null) {
                    ContextHandler contextHandler = new ContextHandler("/doc");
                    ResourceHandler docFileHandler = new ResourceHandler();
                    docFileHandler.setDirectoriesListed(false);
                    docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                    docFileHandler.setResourceBase(javadocResourceBase);
                    contextHandler.setHandler(docFileHandler);
                    apiHandlers.addHandler(contextHandler);
                }

                ServletHandler apiHandler = new ServletHandler();
                apiHandler.addServletWithMapping(APIServlet.class, "/nxt");

                if (Nxt.getBooleanProperty("nxt.apiServerCORS")) {
                    FilterHolder filterHolder = apiHandler.addFilterWithMapping(CrossOriginFilter.class, "/*", FilterMapping.DEFAULT);
                    filterHolder.setInitParameter("allowedHeaders", "*");
                    filterHolder.setAsyncSupported(true);
                }

                apiHandlers.addHandler(apiHandler);
                apiHandlers.addHandler(new DefaultHandler());

                apiServer.setHandler(apiHandlers);
                apiServer.setStopAtShutdown(true);
                apiServer.start();
                Logger.logMessage("Started API server at " + host + ":" + port);
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
