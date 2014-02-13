package nxt;

import nxt.http.HttpRequestDispatcher;
import nxt.peer.Hallmark;
import nxt.peer.HttpJSONRequestHandler;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.user.UserRequestHandler;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Nxt extends HttpServlet {

    public static final String VERSION = "0.7.5";

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

    // /*final*/ variables are set in the init() and are to be treated as final
    public static /*final*/ String myPlatform;
    public static /*final*/ String myScheme;
    public static /*final*/ String myAddress;
    public static /*final*/ String myHallmark;
    public static /*final*/ int myPort;
    public static /*final*/ boolean shareMyAddress;
    public static /*final*/ Set<String> allowedUserHosts;
    public static /*final*/ Set<String> allowedBotHosts;
    public static /*final*/ int blacklistingPeriod;

    public static final int LOGGING_MASK_EXCEPTIONS = 1;
    public static final int LOGGING_MASK_NON200_RESPONSES = 2;
    public static final int LOGGING_MASK_200_RESPONSES = 4;
    public static /*final*/ int communicationLoggingMask;

    public static /*final*/ Set<String> wellKnownPeers;
    public static /*final*/ int maxNumberOfConnectedPublicPeers;
    public static /*final*/ int connectTimeout;
    public static /*final*/ int readTimeout;
    public static /*final*/ boolean enableHallmarkProtection;
    public static /*final*/ int pushThreshold;
    public static /*final*/ int pullThreshold;
    public static /*final*/ int sendToPeersLimit;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        Logger.logMessage("NRS " + VERSION + " starting...");
        if (Logger.debug) {
            Logger.logMessage("DEBUG logging enabled");
        }
        if (Logger.enableStackTraces) {
            Logger.logMessage("logging of exception stack traces enabled");
        }

        try {

            myPlatform = servletConfig.getInitParameter("myPlatform");
            Logger.logMessage("\"myPlatform\" = \"" + myPlatform + "\"");
            if (myPlatform == null) {

                myPlatform = "PC";

            } else {

                myPlatform = myPlatform.trim();

            }

            myScheme = servletConfig.getInitParameter("myScheme");
            Logger.logMessage("\"myScheme\" = \"" + myScheme + "\"");
            if (myScheme == null) {

                myScheme = "http";

            } else {

                myScheme = myScheme.trim();

            }

            String myPort = servletConfig.getInitParameter("myPort");
            Logger.logMessage("\"myPort\" = \"" + myPort + "\"");
            try {

                Nxt.myPort = Integer.parseInt(myPort);

            } catch (NumberFormatException e) {

                Nxt.myPort = myScheme.equals("https") ? 7875 : 7874;
                Logger.logMessage("Invalid value for myPort " + myPort + ", using default " + Nxt.myPort);
            }

            myAddress = servletConfig.getInitParameter("myAddress");
            Logger.logMessage("\"myAddress\" = \"" + myAddress + "\"");
            if (myAddress != null) {

                myAddress = myAddress.trim();

            }

            String shareMyAddress = servletConfig.getInitParameter("shareMyAddress");
            Logger.logMessage("\"shareMyAddress\" = \"" + shareMyAddress + "\"");
            Nxt.shareMyAddress = Boolean.parseBoolean(shareMyAddress);

            myHallmark = servletConfig.getInitParameter("myHallmark");
            Logger.logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
            if (myHallmark != null && (myHallmark = myHallmark.trim()).length() > 0) {

                try {
                    Hallmark hallmark = Hallmark.parseHallmark(myHallmark);
                    if (! hallmark.isValid()) {
                        throw new RuntimeException();
                    }
                } catch (RuntimeException e) {
                    Logger.logMessage("Your hallmark is invalid: " + myHallmark);
                    System.exit(1);
                }

            }

            String wellKnownPeers = servletConfig.getInitParameter("wellKnownPeers");
            Logger.logMessage("\"wellKnownPeers\" = \"" + wellKnownPeers + "\"");
            if (wellKnownPeers != null) {
                Set<String> set = new HashSet<>();
                for (String wellKnownPeer : wellKnownPeers.split(";")) {

                    wellKnownPeer = wellKnownPeer.trim();
                    if (wellKnownPeer.length() > 0) {

                        set.add(wellKnownPeer);
                        Peer.addPeer(wellKnownPeer, wellKnownPeer);

                    }

                }
                Nxt.wellKnownPeers = Collections.unmodifiableSet(set);
            } else {
                Nxt.wellKnownPeers = Collections.emptySet();
                Logger.logMessage("No wellKnownPeers defined, it is unlikely to work");
            }

            String maxNumberOfConnectedPublicPeers = servletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
            Logger.logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + maxNumberOfConnectedPublicPeers + "\"");
            try {

                Nxt.maxNumberOfConnectedPublicPeers = Integer.parseInt(maxNumberOfConnectedPublicPeers);

            } catch (NumberFormatException e) {

                Nxt.maxNumberOfConnectedPublicPeers = 10;
                Logger.logMessage("Invalid value for maxNumberOfConnectedPublicPeers " + maxNumberOfConnectedPublicPeers + ", using default " + Nxt.maxNumberOfConnectedPublicPeers);
            }

            String connectTimeout = servletConfig.getInitParameter("connectTimeout");
            Logger.logMessage("\"connectTimeout\" = \"" + connectTimeout + "\"");
            try {

                Nxt.connectTimeout = Integer.parseInt(connectTimeout);

            } catch (NumberFormatException e) {

                Nxt.connectTimeout = 1000;
                Logger.logMessage("Invalid value for connectTimeout " + connectTimeout + ", using default " + Nxt.connectTimeout);
            }

            String readTimeout = servletConfig.getInitParameter("readTimeout");
            Logger.logMessage("\"readTimeout\" = \"" + readTimeout + "\"");
            try {

                Nxt.readTimeout = Integer.parseInt(readTimeout);

            } catch (NumberFormatException e) {

                Nxt.readTimeout = 1000;
                Logger.logMessage("Invalid value for readTimeout " + readTimeout + ", using default " + Nxt.readTimeout);
            }

            String enableHallmarkProtection = servletConfig.getInitParameter("enableHallmarkProtection");
            Logger.logMessage("\"enableHallmarkProtection\" = \"" + enableHallmarkProtection + "\"");
            Nxt.enableHallmarkProtection = Boolean.parseBoolean(enableHallmarkProtection);

            String pushThreshold = servletConfig.getInitParameter("pushThreshold");
            Logger.logMessage("\"pushThreshold\" = \"" + pushThreshold + "\"");
            try {

                Nxt.pushThreshold = Integer.parseInt(pushThreshold);

            } catch (NumberFormatException e) {

                Nxt.pushThreshold = 0;
                Logger.logMessage("Invalid value for pushThreshold " + pushThreshold + ", using default " + Nxt.pushThreshold);
            }

            String pullThreshold = servletConfig.getInitParameter("pullThreshold");
            Logger.logMessage("\"pullThreshold\" = \"" + pullThreshold + "\"");
            try {

                Nxt.pullThreshold = Integer.parseInt(pullThreshold);

            } catch (NumberFormatException e) {

                Nxt.pullThreshold = 0;
                Logger.logMessage("Invalid value for pullThreshold " + pullThreshold + ", using default " + Nxt.pullThreshold);

            }

            String allowedUserHosts = servletConfig.getInitParameter("allowedUserHosts");
            Logger.logMessage("\"allowedUserHosts\" = \"" + allowedUserHosts + "\"");
            if (allowedUserHosts != null) {

                if (!allowedUserHosts.trim().equals("*")) {

                    Set<String> set = new HashSet<>();
                    for (String allowedUserHost : allowedUserHosts.split(";")) {

                        allowedUserHost = allowedUserHost.trim();
                        if (allowedUserHost.length() > 0) {

                            set.add(allowedUserHost);

                        }

                    }
                    Nxt.allowedUserHosts = Collections.unmodifiableSet(set);

                }

            }

            String allowedBotHosts = servletConfig.getInitParameter("allowedBotHosts");
            Logger.logMessage("\"allowedBotHosts\" = \"" + allowedBotHosts + "\"");
            if (allowedBotHosts != null) {

                if (!allowedBotHosts.trim().equals("*")) {

                    Set<String> set = new HashSet<>();
                    for (String allowedBotHost : allowedBotHosts.split(";")) {

                        allowedBotHost = allowedBotHost.trim();
                        if (allowedBotHost.length() > 0) {

                            set.add(allowedBotHost);

                        }

                    }
                    Nxt.allowedBotHosts = Collections.unmodifiableSet(set);
                }

            }

            String blacklistingPeriod = servletConfig.getInitParameter("blacklistingPeriod");
            Logger.logMessage("\"blacklistingPeriod\" = \"" + blacklistingPeriod + "\"");
            try {

                Nxt.blacklistingPeriod = Integer.parseInt(blacklistingPeriod);

            } catch (NumberFormatException e) {

                Nxt.blacklistingPeriod = 300000;
                Logger.logMessage("Invalid value for blacklistingPeriod " + blacklistingPeriod + ", using default " + Nxt.blacklistingPeriod);

            }

            String communicationLoggingMask = servletConfig.getInitParameter("communicationLoggingMask");
            Logger.logMessage("\"communicationLoggingMask\" = \"" + communicationLoggingMask + "\"");
            try {

                Nxt.communicationLoggingMask = Integer.parseInt(communicationLoggingMask);

            } catch (NumberFormatException e) {
                Logger.logMessage("Invalid value for communicationLogginMask " + communicationLoggingMask + ", using default 0");
            }

            String sendToPeersLimit = servletConfig.getInitParameter("sendToPeersLimit");
            Logger.logMessage("\"sendToPeersLimit\" = \"" + sendToPeersLimit + "\"");
            try {

                Nxt.sendToPeersLimit = Integer.parseInt(sendToPeersLimit);

            } catch (NumberFormatException e) {
                Nxt.sendToPeersLimit = 10;
                Logger.logMessage("Invalid value for sendToPeersLimit " + sendToPeersLimit + ", using default " + Nxt.sendToPeersLimit);
            }

            Db.init();

            Blockchain.init();

            ThreadPools.start();

            Logger.logMessage("NRS " + Nxt.VERSION + " started successfully.");

        } catch (Exception e) {

            Logger.logMessage("Error initializing Nxt servlet", e);
            System.exit(1);

        }

    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        User user = null;

        try {

            String userPasscode = req.getParameter("user");

            if (userPasscode == null) {
                HttpRequestDispatcher.process(req, resp);
                return;
            }

            if (Nxt.allowedUserHosts != null && !Nxt.allowedUserHosts.contains(req.getRemoteHost())) {
                JSONObject response = new JSONObject();
                response.put("response", "denyAccess");
                JSONArray responses = new JSONArray();
                responses.add(response);
                JSONObject combinedResponse = new JSONObject();
                combinedResponse.put("responses", responses);
                resp.setContentType("text/plain; charset=UTF-8");
                try (Writer writer = resp.getWriter()) {
                    combinedResponse.writeJSONString(writer);
                }
                return;
            }

            user = User.getUser(userPasscode);
            UserRequestHandler.process(req, user);

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

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        HttpJSONRequestHandler.process(req, resp);

    }

    @Override
    public void destroy() {

        ThreadPools.shutdown();

        Db.shutdown();

        Logger.logMessage("NRS " + Nxt.VERSION + " stopped.");

    }

}
