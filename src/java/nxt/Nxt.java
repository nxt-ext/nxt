package nxt;

import nxt.http.APIServlet;
import nxt.peer.Hallmark;
import nxt.peer.Peer;
import nxt.user.UserServlet;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class Nxt {

    public static final String VERSION = "0.8.0";

    public static final int BLOCK_HEADER_LENGTH = 224;
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 128;
    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;

    public static final int ALIAS_SYSTEM_BLOCK = 22000;
    public static final int TRANSPARENT_FORGING_BLOCK = 30000;
    public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
    public static final int TRANSPARENT_FORGING_BLOCK_3 = 51000;
    public static final int TRANSPARENT_FORGING_BLOCK_4 = 64000;
    public static final int TRANSPARENT_FORGING_BLOCK_5 = 67000;
    public static final int ASSET_EXCHANGE_BLOCK = 111111;
    public static final int VOTING_SYSTEM_BLOCK = 222222;

    public static final long MAX_BALANCE = 1000000000;
    public static final long initialBaseTarget = 153722867;
    public static final long maxBaseTarget = MAX_BALANCE * initialBaseTarget;
    public static final long MAX_ASSET_QUANTITY = 1000000000;
    public static final int ASSET_ISSUANCE_FEE = 1000;
    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;
    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;

    public static final long epochBeginning;
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.ZONE_OFFSET, 0);
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        epochBeginning = calendar.getTimeInMillis();
    }

    public static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static final int DEFAULT_PEER_PORT = 7874;
    public static final int DEFAULT_API_PORT = 7876;
    public static final int DEFAULT_UI_PORT = 7875;

    public static final String myPlatform;
    public static final String myAddress;
    public static final int myPeerPort;
    public static final String myHallmark;
    public static final boolean shareMyAddress;
    public static final Set<String> allowedUserHosts;
    public static final Set<String> allowedBotHosts;
    public static final int blacklistingPeriod;

    public static final int LOGGING_MASK_EXCEPTIONS = 1;
    public static final int LOGGING_MASK_NON200_RESPONSES = 2;
    public static final int LOGGING_MASK_200_RESPONSES = 4;
    public static final int communicationLoggingMask;

    public static final Set<String> wellKnownPeers;
    public static final int maxNumberOfConnectedPublicPeers;
    public static final int connectTimeout;
    public static final int readTimeout;
    public static final boolean enableHallmarkProtection;
    public static final int pushThreshold;
    public static final int pullThreshold;
    public static final int sendToPeersLimit;

    public static final boolean debug;
    public static final boolean enableStackTraces;
    public static final String logFile;

    private static final Properties properties = new Properties();

    private static final Thread shutdownThread = new Thread(new Runnable() {
        @Override
        public void run() {
            shutdown();
        }
    });

    static {

        long begin = System.currentTimeMillis();
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nxt.properties")) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }

        debug = "true".equals(properties.getProperty("nxt.debug"));
        enableStackTraces = "true".equals(properties.getProperty("nxt.enableStackTraces"));
        logFile = properties.getProperty("log") != null ? properties.getProperty("nxt.log") : "nxt.log";

        Logger.logMessage("Initializing Nxt server version " + VERSION);
        Logger.logDebugMessage("DEBUG logging enabled");
        if (enableStackTraces) {
            Logger.logMessage("logging of exception stack traces enabled");
        }

        if (! getBooleanProperty("nxt.debugJetty", false)) {
            System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        }

        myPlatform = getStringProperty("nxt.myPlatform", "PC");
        myAddress = getStringProperty("nxt.myAddress", null);
        myPeerPort = getIntProperty("nxt.myPeerPort", 7874);
        shareMyAddress = getBooleanProperty("nxt.shareMyAddress", true);
        myHallmark = getStringProperty("nxt.myHallmark", null);
        if (myHallmark != null && myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(myHallmark);
                if (! hallmark.isValid()) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                Logger.logMessage("Your hallmark is invalid: " + myHallmark);
                throw e;
            }
        }

        String wellKnownPeersString = getStringProperty("nxt.wellKnownPeers", null);
        Set<String> addresses = new HashSet<>();
        if (wellKnownPeersString != null) {
            for (String address : wellKnownPeersString.split(";")) {
                address = address.trim();
                if (address.length() > 0) {
                    addresses.add(address);
                }
            }
        } else {
            Logger.logMessage("No wellKnownPeers defined, using default nxtcrypto.org and nxtbase.com nodes");
            for (int i = 1; i <= 12; i++) {
                addresses.add("vps" + i + ".nxtcrypto.org");
            }
            for (int i = 1; i <= 99; i++) {
                addresses.add("node" + i + ".nxtbase.com");
            }
        }
        wellKnownPeers = Collections.unmodifiableSet(addresses);

        maxNumberOfConnectedPublicPeers = getIntProperty("nxt.maxNumberOfConnectedPublicPeers", 20);
        connectTimeout = getIntProperty("nxt.connectTimeout", 2000);
        readTimeout = getIntProperty("nxt.readTimeout", 5000);
        enableHallmarkProtection = getBooleanProperty("nxt.enableHallmarkProtection", true);
        pushThreshold = getIntProperty("nxt.pushThreshold", 0);
        pullThreshold = getIntProperty("nxt.pullThreshold", 0);

        String allowedUserHostsString = getStringProperty("nxt.allowedUserHosts", "127.0.0.1; localhost; 0:0:0:0:0:0:0:1;");
        if (! allowedUserHostsString.equals("*")) {
            Set<String> hosts = new HashSet<>();
            for (String allowedUserHost : allowedUserHostsString.split(";")) {
                allowedUserHost = allowedUserHost.trim();
                if (allowedUserHost.length() > 0) {
                    hosts.add(allowedUserHost);
                }
            }
            allowedUserHosts = Collections.unmodifiableSet(hosts);
        } else {
            allowedUserHosts = null;
        }

        String allowedBotHostsString = getStringProperty("nxt.allowedBotHosts", "127.0.0.1; localhost; 0:0:0:0:0:0:0:1;");
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

        blacklistingPeriod = getIntProperty("nxt.blacklistingPeriod", 300000);
        communicationLoggingMask = getIntProperty("nxt.communicationLoggingMask", 0);
        sendToPeersLimit = getIntProperty("nxt.sendToPeersLimit", 10);

        Runtime.getRuntime().addShutdownHook(shutdownThread);
        Db.init();
        Blockchain.init();
        Peer.init();
        Generator.init();

        for (String address : wellKnownPeers) {
            Peer.addPeer(address);
        }

        boolean enableAPIServer = getBooleanProperty("nxt.enableAPIServer", allowedBotHosts == null || ! allowedBotHosts.isEmpty());
        if (enableAPIServer) {
            startAPIServer();
            Logger.logMessage("Started API server on port " + Nxt.DEFAULT_API_PORT);
        } else {
            Logger.logMessage("API server not enabled");
        }

        boolean enableUIServer = getBooleanProperty("nxt.enableUIServer", allowedUserHosts == null || ! allowedUserHosts.isEmpty());
        if (enableUIServer) {
            startUIServer();
            Logger.logMessage("Started user interface server on port " + Nxt.DEFAULT_UI_PORT);
        } else {
            Logger.logMessage("User interface not enabled");
        }

        long end = System.currentTimeMillis();
        Logger.logDebugMessage("Initialization took " + (end - begin) / 1000 + " seconds");
        Logger.logMessage("Nxt server " + Nxt.VERSION + " started successfully.");

    }

    public static int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined, using default " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringProperty(String name, String defaultValue) {
        String value = properties.getProperty(name);
        if (value != null) {
            Logger.logMessage(name + " = \"" + value.trim() + "\"");
            return value.trim();
        } else {
            Logger.logMessage(name + " not defined, using default " + defaultValue);
            return defaultValue;
        }
    }

    public static Boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, using default " + defaultValue);
        return defaultValue;
    }

    public static void init() {}

    public static void shutdown() {
        Peer.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logMessage("Nxt server " + Nxt.VERSION + " stopped.");
    }

    public static void main(String[] args) {}

    private static void startAPIServer() {
        try {
            Server apiServer = new Server(Nxt.DEFAULT_API_PORT);
            ServletHandler apiHandler = new ServletHandler();
            apiHandler.addServletWithMapping(APIServlet.class, "/nxt");

            ResourceHandler apiFileHandler = new ResourceHandler();
            apiFileHandler.setDirectoriesListed(true);
            apiFileHandler.setWelcomeFiles(new String[]{"index.html"});
            apiFileHandler.setResourceBase("html/tools");

            HandlerList apiHandlers = new HandlerList();
            apiHandlers.setHandlers(new Handler[] { apiFileHandler, apiHandler, new DefaultHandler() });

            apiServer.setHandler(apiHandlers);
            apiServer.start();
        } catch (Exception e) {
            Logger.logDebugMessage("Failed to start API server", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static void startUIServer() {
        try {
            Server userServer = new Server(Nxt.DEFAULT_UI_PORT);

            ServletHandler userHandler = new ServletHandler();
            ServletHolder userHolder = userHandler.addServletWithMapping(UserServlet.class, "/nxt");
            userHolder.setAsyncSupported(true);

            ResourceHandler userFileHandler = new ResourceHandler();
            userFileHandler.setDirectoriesListed(false);
            userFileHandler.setWelcomeFiles(new String[]{"index.html"});
            userFileHandler.setResourceBase("html/nrs");

            HandlerList userHandlers = new HandlerList();
            userHandlers.setHandlers(new Handler[] { userFileHandler, userHandler, new DefaultHandler() });

            userServer.setHandler(userHandlers);
            userServer.start();
        } catch (Exception e) {
            Logger.logDebugMessage("Failed to start user interface server", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

}
