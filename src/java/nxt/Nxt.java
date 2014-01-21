package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class Nxt extends HttpServlet {

    public static final String VERSION = "0.6.0";

    public static final long GENESIS_BLOCK_ID = 2680262203532249785L;
    public static final long CREATOR_ID = 1739068987193023818L;
    public static final byte[] CREATOR_PUBLIC_KEY = {18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27};
    public static final int BLOCK_HEADER_LENGTH = 224;
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 128;
    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;

    public static final int ALIAS_SYSTEM_BLOCK = 22000;
    public static final int TRANSPARENT_FORGING_BLOCK = 30000;
    public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
    public static final byte[] CHECKSUM_TRANSPARENT_FORGING = new byte[]{27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112};

    public static final long MAX_BALANCE = 1000000000;
    public static final long initialBaseTarget = 153722867;
    public static final long maxBaseTarget = MAX_BALANCE * initialBaseTarget;
    public static final long MAX_ASSET_QUANTITY = 1000000000;

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
    static /*final*/ String myPlatform;
    static String myScheme;
    static String myAddress;
    static String myHallmark;
    static /*final*/ int myPort;
    static /*final*/ boolean shareMyAddress;
    private static /*final*/ Set<String> allowedUserHosts, allowedBotHosts;
    private static /*final*/ int blacklistingPeriod;

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static /*final*/ int communicationLoggingMask;

    static final AtomicInteger transactionCounter = new AtomicInteger();
    static final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap<>();

    static /*final*/ Set<String> wellKnownPeers;
    private static /*final*/ int maxNumberOfConnectedPublicPeers;
    static /*final*/ int connectTimeout;
    static int readTimeout;
    static /*final*/ boolean enableHallmarkProtection;
    static /*final*/ int pushThreshold;
    static int pullThreshold;
    static /*final*/ int sendToPeersLimit;
    static final AtomicInteger peerCounter = new AtomicInteger();
    static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();

    static final Object blocksAndTransactionsLock = new Object();

    static final AtomicInteger blockCounter = new AtomicInteger();
    static final ConcurrentMap<Long, Block> blocks = new ConcurrentHashMap<>();
    static final AtomicReference<Block> lastBlock = new AtomicReference<>();
    private static volatile Peer lastBlockchainFeeder;

    static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();

    static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();

    static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap<>();
    static final ConcurrentMap<String, Long> assetNameToIdMappings = new ConcurrentHashMap<>();

    static final ConcurrentMap<Long, AskOrder> askOrders = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, BidOrder> bidOrders = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TreeSet<AskOrder>> sortedAskOrders = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, TreeSet<BidOrder>> sortedBidOrders = new ConcurrentHashMap<>();

    static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(8);

    static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);


    static int getEpochTime(long time) {

        return (int)((time - epochBeginning + 500) / 1000);

    }

    // this is called within block.analyze only, which is already inside the big blocksAndTransactions lock
    static void matchOrders(long assetId) {

        TreeSet<AskOrder> sortedAskOrders = Nxt.sortedAskOrders.get(assetId);
        TreeSet<BidOrder> sortedBidOrders = Nxt.sortedBidOrders.get(assetId);

        while (!sortedAskOrders.isEmpty() && !sortedBidOrders.isEmpty()) {

            AskOrder askOrder = sortedAskOrders.first();
            BidOrder bidOrder = sortedBidOrders.first();

            if (askOrder.price > bidOrder.price) {

                break;

            }

            int quantity = askOrder.quantity < bidOrder.quantity ? askOrder.quantity : bidOrder.quantity;
            long price = askOrder.height < bidOrder.height || (askOrder.height == bidOrder.height && askOrder.id < bidOrder.id) ? askOrder.price : bidOrder.price;

            if ((askOrder.quantity -= quantity) == 0) {

                askOrders.remove(askOrder.id);
                sortedAskOrders.remove(askOrder);

            }

            askOrder.account.addToBalanceAndUnconfirmedBalance(quantity * price);

            if ((bidOrder.quantity -= quantity) == 0) {

                bidOrders.remove(bidOrder.id);
                sortedBidOrders.remove(bidOrder);

            }

            bidOrder.account.addToAssetAndUnconfirmedAssetBalance(assetId, quantity);

        }

    }

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

            //String blockchainStoragePath = servletConfig.getInitParameter("blockchainStoragePath");
            //logMessage("\"blockchainStoragePath\" = \"" + blockchainStoragePath + "\"");
            //blockchainChannel = FileChannel.open(Paths.get(blockchainStoragePath), StandardOpenOption.READ, StandardOpenOption.WRITE);

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
            if (myHallmark != null) {

                myHallmark = myHallmark.trim();

                try {
                    Convert.convert(myHallmark); // check for parsing exceptions
                } catch (NumberFormatException e) {
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

            try {

                Logger.logMessage("Loading transactions...");
                Transaction.loadTransactions("transactions.nxt");
                Logger.logMessage("...Done");

            } catch (FileNotFoundException e) {
                Logger.logMessage("transactions.nxt not found, starting from scratch");
                transactions.clear();

                long[] recipients = {(new BigInteger("163918645372308887")).longValue(),
                        (new BigInteger("620741658595224146")).longValue(),
                        (new BigInteger("723492359641172834")).longValue(),
                        (new BigInteger("818877006463198736")).longValue(),
                        (new BigInteger("1264744488939798088")).longValue(),
                        (new BigInteger("1600633904360147460")).longValue(),
                        (new BigInteger("1796652256451468602")).longValue(),
                        (new BigInteger("1814189588814307776")).longValue(),
                        (new BigInteger("1965151371996418680")).longValue(),
                        (new BigInteger("2175830371415049383")).longValue(),
                        (new BigInteger("2401730748874927467")).longValue(),
                        (new BigInteger("2584657662098653454")).longValue(),
                        (new BigInteger("2694765945307858403")).longValue(),
                        (new BigInteger("3143507805486077020")).longValue(),
                        (new BigInteger("3684449848581573439")).longValue(),
                        (new BigInteger("4071545868996394636")).longValue(),
                        (new BigInteger("4277298711855908797")).longValue(),
                        (new BigInteger("4454381633636789149")).longValue(),
                        (new BigInteger("4747512364439223888")).longValue(),
                        (new BigInteger("4777958973882919649")).longValue(),
                        (new BigInteger("4803826772380379922")).longValue(),
                        (new BigInteger("5095742298090230979")).longValue(),
                        (new BigInteger("5271441507933314159")).longValue(),
                        (new BigInteger("5430757907205901788")).longValue(),
                        (new BigInteger("5491587494620055787")).longValue(),
                        (new BigInteger("5622658764175897611")).longValue(),
                        (new BigInteger("5982846390354787993")).longValue(),
                        (new BigInteger("6290807999797358345")).longValue(),
                        (new BigInteger("6785084810899231190")).longValue(),
                        (new BigInteger("6878906112724074600")).longValue(),
                        (new BigInteger("7017504655955743955")).longValue(),
                        (new BigInteger("7085298282228890923")).longValue(),
                        (new BigInteger("7446331133773682477")).longValue(),
                        (new BigInteger("7542917420413518667")).longValue(),
                        (new BigInteger("7549995577397145669")).longValue(),
                        (new BigInteger("7577840883495855927")).longValue(),
                        (new BigInteger("7579216551136708118")).longValue(),
                        (new BigInteger("8278234497743900807")).longValue(),
                        (new BigInteger("8517842408878875334")).longValue(),
                        (new BigInteger("8870453786186409991")).longValue(),
                        (new BigInteger("9037328626462718729")).longValue(),
                        (new BigInteger("9161949457233564608")).longValue(),
                        (new BigInteger("9230759115816986914")).longValue(),
                        (new BigInteger("9306550122583806885")).longValue(),
                        (new BigInteger("9433259657262176905")).longValue(),
                        (new BigInteger("9988839211066715803")).longValue(),
                        (new BigInteger("10105875265190846103")).longValue(),
                        (new BigInteger("10339765764359265796")).longValue(),
                        (new BigInteger("10738613957974090819")).longValue(),
                        (new BigInteger("10890046632913063215")).longValue(),
                        (new BigInteger("11494237785755831723")).longValue(),
                        (new BigInteger("11541844302056663007")).longValue(),
                        (new BigInteger("11706312660844961581")).longValue(),
                        (new BigInteger("12101431510634235443")).longValue(),
                        (new BigInteger("12186190861869148835")).longValue(),
                        (new BigInteger("12558748907112364526")).longValue(),
                        (new BigInteger("13138516747685979557")).longValue(),
                        (new BigInteger("13330279748251018740")).longValue(),
                        (new BigInteger("14274119416917666908")).longValue(),
                        (new BigInteger("14557384677985343260")).longValue(),
                        (new BigInteger("14748294830376619968")).longValue(),
                        (new BigInteger("14839596582718854826")).longValue(),
                        (new BigInteger("15190676494543480574")).longValue(),
                        (new BigInteger("15253761794338766759")).longValue(),
                        (new BigInteger("15558257163011348529")).longValue(),
                        (new BigInteger("15874940801139996458")).longValue(),
                        (new BigInteger("16516270647696160902")).longValue(),
                        (new BigInteger("17156841960446798306")).longValue(),
                        (new BigInteger("17228894143802851995")).longValue(),
                        (new BigInteger("17240396975291969151")).longValue(),
                        (new BigInteger("17491178046969559641")).longValue(),
                        (new BigInteger("18345202375028346230")).longValue(),
                        (new BigInteger("18388669820699395594")).longValue()};
                int[] amounts = {36742,
                        1970092,
                        349130,
                        24880020,
                        2867856,
                        9975150,
                        2690963,
                        7648,
                        5486333,
                        34913026,
                        997515,
                        30922966,
                        6650,
                        44888,
                        2468850,
                        49875751,
                        49875751,
                        9476393,
                        49875751,
                        14887912,
                        528683,
                        583546,
                        7315,
                        19925363,
                        29856290,
                        5320,
                        4987575,
                        5985,
                        24912938,
                        49875751,
                        2724712,
                        1482474,
                        200999,
                        1476156,
                        498758,
                        987540,
                        16625250,
                        5264386,
                        15487585,
                        2684479,
                        14962725,
                        34913026,
                        5033128,
                        2916900,
                        49875751,
                        4962637,
                        170486123,
                        8644631,
                        22166945,
                        6668388,
                        233751,
                        4987575,
                        11083556,
                        1845403,
                        49876,
                        3491,
                        3491,
                        9476,
                        49876,
                        6151,
                        682633,
                        49875751,
                        482964,
                        4988,
                        49875751,
                        4988,
                        9144,
                        503745,
                        49875751,
                        52370,
                        29437998,
                        585375,
                        9975150};
                byte[][] signatures = {{41, 115, -41, 7, 37, 21, -3, -41, 120, 119, 63, -101, 108, 48, -117, 1, -43, 32, 85, 95, 65, 42, 92, -22, 123, -36, 6, -99, -61, -53, 93, 7, 23, 8, -30, 65, 57, -127, -2, 42, -92, -104, 11, 72, -66, 108, 17, 113, 99, -117, -75, 123, 110, 107, 119, -25, 67, 64, 32, 117, 111, 54, 82, -14},
                        {118, 43, 84, -91, -110, -102, 100, -40, -33, -47, -13, -7, -88, 2, -42, -66, -38, -22, 105, -42, -69, 78, 51, -55, -48, 49, -89, 116, -96, -104, -114, 14, 94, 58, -115, -8, 111, -44, 76, -104, 54, -15, 126, 31, 6, -80, 65, 6, 124, 37, -73, 92, 4, 122, 122, -108, 1, -54, 31, -38, -117, -1, -52, -56},
                        {79, 100, -101, 107, -6, -61, 40, 32, -98, 32, 80, -59, -76, -23, -62, 38, 4, 105, -106, -105, -121, -85, 13, -98, -77, 126, -125, 103, 12, -41, 1, 2, 45, -62, -69, 102, 116, -61, 101, -14, -68, -31, 9, 110, 18, 2, 33, -98, -37, -128, 17, -19, 124, 125, -63, 92, -70, 96, 96, 125, 91, 8, -65, -12},
                        {58, -99, 14, -97, -75, -10, 110, -102, 119, -3, -2, -12, -82, -33, -27, 118, -19, 55, -109, 6, 110, -10, 108, 30, 94, -113, -5, -98, 19, 12, -125, 14, -77, 33, -128, -21, 36, -120, -12, -81, 64, 95, 67, -3, 100, 122, -47, 127, -92, 114, 68, 72, 2, -40, 80, 117, -17, -56, 103, 37, -119, 3, 22, 23},
                        {76, 22, 121, -4, -77, -127, 18, -102, 7, 94, -73, -96, 108, -11, 81, -18, -37, -85, -75, 86, -119, 94, 126, 95, 47, -36, -16, -50, -9, 95, 60, 15, 14, 93, -108, -83, -67, 29, 2, -53, 10, -118, -51, -46, 92, -23, -56, 60, 46, -90, -84, 126, 60, 78, 12, 53, 61, 121, -6, 77, 112, 60, 40, 63},
                        {64, 121, -73, 68, 4, -103, 81, 55, -41, -81, -63, 10, 117, -74, 54, -13, -85, 79, 21, 116, -25, -12, 21, 120, -36, -80, 53, -78, 103, 25, 55, 6, -75, 96, 80, -125, -11, -103, -20, -41, 121, -61, -40, 63, 24, -81, -125, 90, -12, -40, -52, -1, -114, 14, -44, -112, -80, 83, -63, 88, -107, -10, -114, -86},
                        {-81, 126, -41, -34, 66, -114, -114, 114, 39, 32, -125, -19, -95, -50, -111, -51, -33, 51, 99, -127, 58, 50, -110, 44, 80, -94, -96, 68, -69, 34, 86, 3, -82, -69, 28, 20, -111, 69, 18, -41, -23, 27, -118, 20, 72, 21, -112, 53, -87, -81, -47, -101, 123, -80, 99, -15, 33, -120, -8, 82, 80, -8, -10, -45},
                        {92, 77, 53, -87, 26, 13, -121, -39, -62, -42, 47, 4, 7, 108, -15, 112, 103, 38, -50, -74, 60, 56, -63, 43, -116, 49, -106, 69, 118, 65, 17, 12, 31, 127, -94, 55, -117, -29, -117, 31, -95, -110, -2, 63, -73, -106, -88, -41, -19, 69, 60, -17, -16, 61, 32, -23, 88, -106, -96, 37, -96, 114, -19, -99},
                        {68, -26, 57, -56, -30, 108, 61, 24, 106, -56, -92, 99, -59, 107, 25, -110, -57, 80, 79, -92, -107, 90, 54, -73, -40, -39, 78, 109, -57, -62, -17, 6, -25, -29, 37, 90, -24, -27, -61, -69, 44, 121, 107, -72, -57, 108, 32, -69, -21, -41, 126, 91, 118, 11, -91, 50, -11, 116, 126, -96, -39, 110, 105, -52},
                        {48, 108, 123, 50, -50, -58, 33, 14, 59, 102, 17, -18, -119, 4, 10, -29, 36, -56, -31, 43, -71, -48, -14, 87, 119, -119, 40, 104, -44, -76, -24, 2, 48, -96, -7, 16, -119, -3, 108, 78, 125, 88, 61, -53, -3, -16, 20, -83, 74, 124, -47, -17, -15, -21, -23, -119, -47, 105, -4, 115, -20, 77, 57, 88},
                        {33, 101, 79, -35, 32, -119, 20, 120, 34, -80, -41, 90, -22, 93, -20, -45, 9, 24, -46, 80, -55, -9, -24, -78, -124, 27, -120, -36, -51, 59, -38, 7, 113, 125, 68, 109, 24, -121, 111, 37, -71, 100, -111, 78, -43, -14, -76, -44, 64, 103, 16, -28, -44, -103, 74, 81, -118, -74, 47, -77, -65, 8, 42, -100},
                        {-63, -96, -95, -111, -85, -98, -85, 42, 87, 29, -62, -57, 57, 48, 9, -39, -110, 63, -103, -114, -48, -11, -92, 105, -26, -79, -11, 78, -118, 14, -39, 1, -115, 74, 70, -41, -119, -68, -39, -60, 64, 31, 25, -111, -16, -20, 61, -22, 17, -13, 57, -110, 24, 61, -104, 21, -72, -69, 56, 116, -117, 93, -1, -123},
                        {-18, -70, 12, 112, -111, 10, 22, 31, -120, 26, 53, 14, 10, 69, 51, 45, -50, -127, -22, 95, 54, 17, -8, 54, -115, 36, -79, 12, -79, 82, 4, 1, 92, 59, 23, -13, -85, -87, -110, -58, 84, -31, -48, -105, -101, -92, -9, 28, -109, 77, -47, 100, -48, -83, 106, -102, 70, -95, 94, -1, -99, -15, 21, 99},
                        {109, 123, 54, 40, -120, 32, -118, 49, -52, 0, -103, 103, 101, -9, 32, 78, 124, -56, 88, -19, 101, -32, 70, 67, -41, 85, -103, 1, 1, -105, -51, 10, 4, 51, -26, -19, 39, -43, 63, -41, -101, 80, 106, -66, 125, 47, -117, -120, -93, -120, 99, -113, -17, 61, 102, -2, 72, 9, -124, 123, -128, 78, 43, 96},
                        {-22, -63, 20, 65, 5, -89, -123, -61, 14, 34, 83, -113, 34, 85, 26, -21, 1, 16, 88, 55, -92, -111, 14, -31, -37, -67, -8, 85, 39, -112, -33, 8, 28, 16, 107, -29, 1, 3, 100, -53, 2, 81, 52, -94, -14, 36, -123, -82, -6, -118, 104, 75, -99, -82, -100, 7, 30, -66, 0, -59, 108, -54, 31, 20},
                        {0, 13, -74, 28, -54, -12, 45, 36, -24, 55, 43, -110, -72, 117, -110, -56, -72, 85, 79, -89, -92, 65, -67, -34, -24, 38, 67, 42, 84, -94, 91, 13, 100, 89, 20, -95, -76, 2, 116, 34, 67, 52, -80, -101, -22, -32, 51, 32, -76, 44, -93, 11, 42, -69, -12, 7, -52, -55, 122, -10, 48, 21, 92, 110},
                        {-115, 19, 115, 28, -56, 118, 111, 26, 18, 123, 111, -96, -115, 120, 105, 62, -123, -124, 101, 51, 3, 18, -89, 127, 48, -27, 39, -78, -118, 5, -2, 6, -105, 17, 123, 26, 25, -62, -37, 49, 117, 3, 10, 97, -7, 54, 121, -90, -51, -49, 11, 104, -66, 11, -6, 57, 5, -64, -8, 59, 82, -126, 26, -113},
                        {16, -53, 94, 99, -46, -29, 64, -89, -59, 116, -21, 53, 14, -77, -71, 95, 22, -121, -51, 125, -14, -96, 95, 95, 32, 96, 79, 41, -39, -128, 79, 0, 5, 6, -115, 104, 103, 77, -92, 93, -109, 58, 96, 97, -22, 116, -62, 11, 30, -122, 14, 28, 69, 124, 63, -119, 19, 80, -36, -116, -76, -58, 36, 87},
                        {109, -82, 33, -119, 17, 109, -109, -16, 98, 108, 111, 5, 98, 1, -15, -32, 22, 46, -65, 117, -78, 119, 35, -35, -3, 41, 23, -97, 55, 69, 58, 9, 20, -113, -121, -13, -41, -48, 22, -73, -1, -44, -73, 3, -10, -122, 19, -103, 10, -26, -128, 62, 34, 55, 54, -43, 35, -30, 115, 64, -80, -20, -25, 67},
                        {-16, -74, -116, -128, 52, 96, -75, 17, -22, 72, -43, 22, -95, -16, 32, -72, 98, 46, -4, 83, 34, -58, -108, 18, 17, -58, -123, 53, -108, 116, 18, 2, 7, -94, -126, -45, 72, -69, -65, -89, 64, 31, -78, 78, -115, 106, 67, 55, -123, 104, -128, 36, -23, -90, -14, -87, 78, 19, 18, -128, 39, 73, 35, 120},
                        {20, -30, 15, 111, -82, 39, -108, 57, -80, 98, -19, -27, 100, -18, 47, 77, -41, 95, 80, -113, -128, -88, -76, 115, 65, -53, 83, 115, 7, 2, -104, 3, 120, 115, 14, -116, 33, -15, -120, 22, -56, -8, -69, 5, -75, 94, 124, 12, -126, -48, 51, -105, 22, -66, -93, 16, -63, -74, 32, 114, -54, -3, -47, -126},
                        {56, -101, 55, -1, 64, 4, -64, 95, 31, -15, 72, 46, 67, -9, 68, -43, -55, 28, -63, -17, -16, 65, 11, -91, -91, 32, 88, 41, 60, 67, 105, 8, 58, 102, -79, -5, -113, -113, -67, 82, 50, -26, 116, -78, -103, 107, 102, 23, -74, -47, 115, -50, -35, 63, -80, -32, 72, 117, 47, 68, 86, -20, -35, 8},
                        {21, 27, 20, -59, 117, -102, -42, 22, -10, 121, 41, -59, 115, 15, -43, 54, -79, -62, -16, 58, 116, 15, 88, 108, 114, 67, 3, -30, -99, 78, 103, 11, 49, 63, -4, -110, -27, 41, 70, -57, -69, -18, 70, 30, -21, 66, -104, -27, 3, 53, 50, 100, -33, 54, -3, -78, 92, 85, -78, 54, 19, 32, 95, 9},
                        {-93, 65, -64, -79, 82, 85, -34, -90, 122, -29, -40, 3, -80, -40, 32, 26, 102, -73, 17, 53, -93, -29, 122, 86, 107, -100, 50, 56, -28, 124, 90, 14, 93, -88, 97, 101, -85, -50, 46, -109, -88, -127, -112, 63, -89, 24, -34, -9, -116, -59, -87, -86, -12, 111, -111, 87, -87, -13, -73, -124, -47, 7, 1, 9},
                        {60, -99, -77, -20, 112, -75, -34, 100, -4, -96, 81, 71, -116, -62, 38, -68, 105, 7, -126, 21, -125, -25, -56, -11, -59, 95, 117, 108, 32, -38, -65, 13, 46, 65, -46, -89, 0, 120, 5, 23, 40, 110, 114, 79, 111, -70, 8, 16, -49, -52, -82, -18, 108, -43, 81, 96, 72, -65, 70, 7, -37, 58, 46, -14},
                        {-95, -32, 85, 78, 74, -53, 93, -102, -26, -110, 86, 1, -93, -50, -23, -108, -37, 97, 19, 103, 94, -65, -127, -21, 60, 98, -51, -118, 82, -31, 27, 7, -112, -45, 79, 95, -20, 90, -4, -40, 117, 100, -6, 19, -47, 53, 53, 48, 105, 91, -70, -34, -5, -87, -57, -103, -112, -108, -40, 87, -25, 13, -76, -116},
                        {44, -122, -70, 125, -60, -32, 38, 69, -77, -103, 49, -124, -4, 75, -41, -84, 68, 74, 118, 15, -13, 115, 117, -78, 42, 89, 0, -20, -12, -58, -97, 10, -48, 95, 81, 101, 23, -67, -23, 74, -79, 21, 97, 123, 103, 101, -50, -115, 116, 112, 51, 50, -124, 27, 76, 40, 74, 10, 65, -49, 102, 95, 5, 35},
                        {-6, 57, 71, 5, -61, -100, -21, -9, 47, -60, 59, 108, -75, 105, 56, 41, -119, 31, 37, 27, -86, 120, -125, -108, 121, 104, -21, -70, -57, -104, 2, 11, 118, 104, 68, 6, 7, -90, -70, -61, -16, 77, -8, 88, 31, -26, 35, -44, 8, 50, 51, -88, -62, -103, 54, -41, -2, 117, 98, -34, 49, -123, 83, -58},
                        {54, 21, -36, 126, -50, -72, 82, -5, -122, -116, 72, -19, -18, -68, -71, -27, 97, -22, 53, -94, 47, -6, 15, -92, -55, 127, 13, 13, -69, 81, -82, 8, -50, 10, 84, 110, -87, -44, 61, -78, -65, 84, -32, 48, -8, -105, 35, 116, -68, -116, -6, 75, -77, 120, -95, 74, 73, 105, 39, -87, 98, -53, 47, 10},
                        {-113, 116, 37, -1, 95, -89, -93, 113, 36, -70, -57, -99, 94, 52, -81, -118, 98, 58, -36, 73, 82, -67, -80, 46, 83, -127, -8, 73, 66, -27, 43, 7, 108, 32, 73, 1, -56, -108, 41, -98, -15, 49, 1, 107, 65, 44, -68, 126, -28, -53, 120, -114, 126, -79, -14, -105, -33, 53, 5, -119, 67, 52, 35, -29},
                        {98, 23, 23, 83, 78, -89, 13, 55, -83, 97, -30, -67, 99, 24, 47, -4, -117, -34, -79, -97, 95, 74, 4, 21, 66, -26, 15, 80, 60, -25, -118, 14, 36, -55, -41, -124, 90, -1, 84, 52, 31, 88, 83, 121, -47, -59, -10, 17, 51, -83, 23, 108, 19, 104, 32, 29, -66, 24, 21, 110, 104, -71, -23, -103},
                        {12, -23, 60, 35, 6, -52, -67, 96, 15, -128, -47, -15, 40, 3, 54, -81, 3, 94, 3, -98, -94, -13, -74, -101, 40, -92, 90, -64, -98, 68, -25, 2, -62, -43, 112, 32, -78, -123, 26, -80, 126, 120, -88, -92, 126, -128, 73, -43, 87, -119, 81, 111, 95, -56, -128, -14, 51, -40, -42, 102, 46, 106, 6, 6},
                        {-38, -120, -11, -114, -7, -105, -98, 74, 114, 49, 64, -100, 4, 40, 110, -21, 25, 6, -92, -40, -61, 48, 94, -116, -71, -87, 75, -31, 13, -119, 1, 5, 33, -69, -16, -125, -79, -46, -36, 3, 40, 1, -88, -118, -107, 95, -23, -107, -49, 44, -39, 2, 108, -23, 39, 50, -51, -59, -4, -42, -10, 60, 10, -103},
                        {67, -53, 55, -32, -117, 3, 94, 52, -115, -127, -109, 116, -121, -27, -115, -23, 98, 90, -2, 48, -54, -76, 108, -56, 99, 30, -35, -18, -59, 25, -122, 3, 43, -13, -109, 34, -10, 123, 117, 113, -112, -85, -119, -62, -78, -114, -96, 101, 72, -98, 28, 89, -98, -121, 20, 115, 89, -20, 94, -55, 124, 27, -76, 94},
                        {15, -101, 98, -21, 8, 5, -114, -64, 74, 123, 99, 28, 125, -33, 22, 23, -2, -56, 13, 91, 27, -105, -113, 19, 60, -7, -67, 107, 70, 103, -107, 13, -38, -108, -77, -29, 2, 9, -12, 21, 12, 65, 108, -16, 69, 77, 96, -54, 55, -78, -7, 41, -48, 124, -12, 64, 113, -45, -21, -119, -113, 88, -116, 113},
                        {-17, 77, 10, 84, -57, -12, 101, 21, -91, 92, 17, -32, -26, 77, 70, 46, 81, -55, 40, 44, 118, -35, -97, 47, 5, 125, 41, -127, -72, 66, -18, 2, 115, -13, -74, 126, 86, 80, 11, -122, -29, -68, 113, 54, -117, 107, -75, -107, -54, 72, -44, 98, -111, -33, -56, -40, 93, -47, 84, -43, -45, 86, 65, -84},
                        {-126, 60, -56, 121, 31, -124, -109, 100, -118, -29, 106, 94, 5, 27, 13, -79, 91, -111, -38, -42, 18, 61, -100, 118, -18, -4, -60, 121, 46, -22, 6, 4, -37, -20, 124, -43, 51, -57, -49, -44, -24, -38, 81, 60, -14, -97, -109, -11, -5, -85, 75, -17, -124, -96, -53, 52, 64, 100, -118, -120, 6, 60, 76, -110},
                        {-12, -40, 115, -41, 68, 85, 20, 91, -44, -5, 73, -105, -81, 32, 116, 32, -28, 69, 88, -54, 29, -53, -51, -83, 54, 93, -102, -102, -23, 7, 110, 15, 34, 122, 84, 52, -121, 37, -103, -91, 37, -77, -101, 64, -18, 63, -27, -75, -112, -11, 1, -69, -25, -123, -99, -31, 116, 11, 4, -42, -124, 98, -2, 53},
                        {-128, -69, -16, -33, -8, -112, 39, -57, 113, -76, -29, -37, 4, 121, -63, 12, -54, -66, -121, 13, -4, -44, 50, 27, 103, 101, 44, -115, 12, -4, -8, 10, 53, 108, 90, -47, 46, -113, 5, -3, -111, 8, -66, -73, 57, 72, 90, -33, 47, 99, 50, -55, 53, -4, 96, 87, 57, 26, 53, -45, -83, 39, -17, 45},
                        {-121, 125, 60, -9, -79, -128, -19, 113, 54, 77, -23, -89, 105, -5, 47, 114, -120, -88, 31, 25, -96, -75, -6, 76, 9, -83, 75, -109, -126, -47, -6, 2, -59, 64, 3, 74, 100, 110, -96, 66, -3, 10, -124, -6, 8, 50, 109, 14, -109, 79, 73, 77, 67, 63, -50, 10, 86, -63, -125, -86, 35, -26, 7, 83},
                        {36, 31, -77, 126, 106, 97, 63, 81, -37, -126, 69, -127, -22, -69, 104, -111, 93, -49, 77, -3, -38, -112, 47, -55, -23, -68, -8, 78, -127, -28, -59, 10, 22, -61, -127, -13, -72, -14, -87, 14, 61, 76, -89, 81, -97, -97, -105, 94, -93, -9, -3, -104, -83, 59, 104, 121, -30, 106, -2, 62, -51, -72, -63, 55},
                        {81, -88, -8, -96, -31, 118, -23, -38, -94, 80, 35, -20, -93, -102, 124, 93, 0, 15, 36, -127, -41, -19, 6, -124, 94, -49, 44, 26, -69, 43, -58, 9, -18, -3, -2, 60, -122, -30, -47, 124, 71, 47, -74, -68, 4, -101, -16, 77, -120, -15, 45, -12, 68, -77, -74, 63, -113, 44, -71, 56, 122, -59, 53, -44},
                        {122, 30, 27, -79, 32, 115, -104, -28, -53, 109, 120, 121, -115, -65, -87, 101, 23, 10, 122, 101, 29, 32, 56, 63, -23, -48, -51, 51, 16, -124, -41, 6, -71, 49, -20, 26, -57, 65, 49, 45, 7, 49, -126, 54, -122, -43, 1, -5, 111, 117, 104, 117, 126, 114, -77, 66, -127, -50, 69, 14, 70, 73, 60, 112},
                        {104, -117, 105, -118, 35, 16, -16, 105, 27, -87, -43, -59, -13, -23, 5, 8, -112, -28, 18, -1, 48, 94, -82, 55, 32, 16, 59, -117, 108, -89, 101, 9, -35, 58, 70, 62, 65, 91, 14, -43, -104, 97, 1, -72, 16, -24, 79, 79, -85, -51, -79, -55, -128, 23, 109, -95, 17, 92, -38, 109, 65, -50, 46, -114},
                        {44, -3, 102, -60, -85, 66, 121, -119, 9, 82, -47, -117, 67, -28, 108, 57, -47, -52, -24, -82, 65, -13, 85, 107, -21, 16, -24, -85, 102, -92, 73, 5, 7, 21, 41, 47, -118, 72, 43, 51, -5, -64, 100, -34, -25, 53, -45, -115, 30, -72, -114, 126, 66, 60, -24, -67, 44, 48, 22, 117, -10, -33, -89, -108},
                        {-7, 71, -93, -66, 3, 30, -124, -116, -48, -76, -7, -62, 125, -122, -60, -104, -30, 71, 36, -110, 34, -126, 31, 10, 108, 102, -53, 56, 104, -56, -48, 12, 25, 21, 19, -90, 45, -122, -73, -112, 97, 96, 115, 71, 127, -7, -46, 84, -24, 102, -104, -96, 28, 8, 37, -84, -13, -65, -6, 61, -85, -117, -30, 70},
                        {-112, 39, -39, -24, 127, -115, 68, -1, -111, -43, 101, 20, -12, 39, -70, 67, -50, 68, 105, 69, -91, -106, 91, 4, -52, 75, 64, -121, 46, -117, 31, 10, -125, 77, 51, -3, -93, 58, 79, 121, 126, -29, 56, -101, 1, -28, 49, 16, -80, 92, -62, 83, 33, 17, 106, 89, -9, 60, 79, 38, -74, -48, 119, 24},
                        {105, -118, 34, 52, 111, 30, 38, -73, 125, -116, 90, 69, 2, 126, -34, -25, -41, -67, -23, -105, -12, -75, 10, 69, -51, -95, -101, 92, -80, -73, -120, 2, 71, 46, 11, -85, -18, 125, 81, 117, 33, -89, -42, 118, 51, 60, 89, 110, 97, -118, -111, -36, 75, 112, -4, -8, -36, -49, -55, 35, 92, 70, -37, 36},
                        {71, 4, -113, 13, -48, 29, -56, 82, 115, -38, -20, -79, -8, 126, -111, 5, -12, -56, -107, 98, 111, 19, 127, -10, -42, 24, -38, -123, 59, 51, -64, 3, 47, -1, -83, -127, -58, 86, 33, -76, 5, 71, -80, -50, -62, 116, 75, 20, -126, 23, -31, -21, 24, -83, -19, 114, -17, 1, 110, -70, -119, 126, 82, -83},
                        {-77, -69, -45, -78, -78, 69, 35, 85, 84, 25, -66, -25, 53, -38, -2, 125, -38, 103, 88, 31, -9, -43, 15, -93, 69, -22, -13, -20, 73, 3, -100, 7, 26, -18, 123, -14, -78, 113, 79, -57, -109, -118, 105, -104, 75, -88, -24, -109, 73, -126, 9, 55, 98, -120, 93, 114, 74, 0, -86, -68, 47, 29, 75, 67},
                        {-104, 11, -85, 16, -124, -91, 66, -91, 18, -67, -122, -57, -114, 88, 79, 11, -60, -119, 89, 64, 57, 120, -11, 8, 52, -18, -67, -127, 26, -19, -69, 2, -82, -56, 11, -90, -104, 110, -10, -68, 87, 21, 28, 87, -5, -74, -21, -84, 120, 70, -17, 102, 72, -116, -69, 108, -86, -79, -74, 115, -78, -67, 6, 45},
                        {-6, -101, -17, 38, -25, -7, -93, 112, 13, -33, 121, 71, -79, -122, -95, 22, 47, -51, 16, 84, 55, -39, -26, 37, -36, -18, 11, 119, 106, -57, 42, 8, -1, 23, 7, -63, -9, -50, 30, 35, -125, 83, 9, -60, -94, -15, -76, 120, 18, -103, -70, 95, 26, 48, -103, -95, 10, 113, 66, 54, -96, -4, 37, 111},
                        {-124, -53, 43, -59, -73, 99, 71, -36, -31, 61, -25, -14, -71, 48, 17, 10, -26, -21, -22, 104, 64, -128, 27, -40, 111, -70, -90, 91, -81, -88, -10, 11, -62, 127, -124, -2, -67, -69, 65, 73, 40, 82, 112, -112, 100, -26, 30, 86, 30, 1, -105, 45, 103, -47, -124, 58, 105, 24, 20, 108, -101, 84, -34, 80},
                        {28, -1, 84, 111, 43, 109, 57, -23, 52, -95, 110, -50, 77, 15, 80, 85, 125, -117, -10, 8, 59, -58, 18, 97, -58, 45, 92, -3, 56, 24, -117, 9, -73, -9, 48, -99, 50, -24, -3, -41, -43, 48, -77, -8, -89, -42, 126, 73, 28, -65, -108, 54, 6, 34, 32, 2, -73, -123, -106, -52, -73, -106, -112, 109},
                        {73, -76, -7, 49, 67, -34, 124, 80, 111, -91, -22, -121, -74, 42, -4, -18, 84, -3, 38, 126, 31, 54, -120, 65, -122, -14, -38, -80, -124, 90, 37, 1, 51, 123, 69, 48, 109, -112, -63, 27, 67, -127, 29, 79, -26, 99, -24, -100, 51, 103, -105, 13, 85, 74, 12, -37, 43, 80, -113, 6, 70, -107, -5, -80},
                        {110, -54, 109, 21, -124, 98, 90, -26, 69, -44, 17, 117, 78, -91, -7, -18, -81, -43, 20, 80, 48, -109, 117, 125, -67, 19, -15, 69, -28, 47, 15, 4, 34, -54, 51, -128, 18, 61, -77, -122, 100, -58, -118, -36, 5, 32, 43, 15, 60, -55, 120, 123, -77, -76, -121, 77, 93, 16, -73, 54, 46, -83, -39, 125},
                        {115, -15, -42, 111, -124, 52, 29, -124, -10, -23, 41, -128, 65, -60, -121, 6, -42, 14, 98, -80, 80, -46, -38, 64, 16, 84, -50, 47, -97, 11, -88, 12, 68, -127, -92, 87, -22, 54, -49, 33, -4, -68, 21, -7, -45, 84, 107, 57, 8, -106, 0, -87, -104, 93, -43, -98, -92, -72, 110, -14, -66, 119, 14, -68},
                        {-19, 7, 7, 66, -94, 18, 36, 8, -58, -31, 21, -113, -124, -5, -12, 105, 40, -62, 57, -56, 25, 117, 49, 17, -33, 49, 105, 113, -26, 78, 97, 2, -22, -84, 49, 67, -6, 33, 89, 28, 30, 12, -3, -23, -45, 7, -4, -39, -20, 25, -91, 55, 53, 21, -94, 17, -54, 109, 125, 124, 122, 117, -125, 60},
                        {-28, -104, -46, -22, 71, -79, 100, 48, -90, -57, -30, -23, -24, 1, 2, -31, 85, -6, -113, -116, 105, -31, -109, 106, 1, 78, -3, 103, -6, 100, -44, 15, -100, 97, 59, -42, 22, 83, 113, -118, 112, -57, 80, -45, -86, 72, 77, -26, -106, 50, 28, -24, 41, 22, -73, 108, 18, -93, 30, 8, -11, -16, 124, 106},
                        {16, -119, -109, 115, 67, 36, 28, 74, 101, -58, -82, 91, 4, -97, 111, -77, -37, -125, 126, 3, 10, -99, -115, 91, -66, -83, -81, 10, 7, 92, 26, 6, -45, 66, -26, 118, -77, 13, -91, 20, -18, -33, -103, 43, 75, -100, -5, -64, 117, 30, 5, -100, -90, 13, 18, -52, 26, 24, -10, 24, -31, 53, 88, 112},
                        {7, -90, 46, 109, -42, 108, -84, 124, -28, -63, 34, -19, -76, 88, -121, 23, 54, -73, -15, -52, 84, -119, 64, 20, 92, -91, -58, -121, -117, -90, -102, 1, 49, 21, 3, -85, -3, 38, 117, 73, -38, -71, -37, 40, -2, -50, -47, -46, 75, -105, 125, 126, -13, 68, 50, -81, -43, -93, 85, -79, 52, 98, 118, 50},
                        {-104, 65, -61, 12, 68, 106, 37, -64, 40, -114, 61, 73, 74, 61, -113, -79, 57, 47, -57, -21, -68, -62, 23, -18, 93, -7, -55, -88, -106, 104, -126, 5, 53, 97, 100, -67, -112, -88, 41, 24, 95, 15, 25, -67, 79, -69, 53, 21, -128, -101, 73, 17, 7, -98, 5, -2, 33, -113, 99, -72, 125, 7, 18, -105},
                        {-17, 28, 79, 34, 110, 86, 43, 27, -114, -112, -126, -98, -121, 126, -21, 111, 58, -114, -123, 75, 117, -116, 7, 107, 90, 80, -75, -121, 116, -11, -76, 0, -117, -52, 76, -116, 115, -117, 61, -7, 55, -34, 38, 101, 86, -19, -36, -92, -94, 61, 88, -128, -121, -103, 84, 19, -83, -102, 122, -111, 62, 112, 20, 3},
                        {-127, -90, 28, -77, -48, -56, -10, 84, -41, 59, -115, 68, -74, -104, -119, -49, -37, -90, -57, 66, 108, 110, -62, -107, 88, 90, 29, -65, 74, -38, 95, 8, 120, 88, 96, -65, -109, 68, -63, -4, -16, 90, 7, 39, -56, -110, -100, 86, -39, -53, -89, -35, 127, -42, -48, -36, 53, -66, 109, -51, 51, -23, -12, 73},
                        {-12, 78, 81, 30, 124, 22, 56, -112, 58, -99, 30, -98, 103, 66, 89, 92, -52, -20, 26, 82, -92, -18, 96, 7, 38, 21, -9, -25, -17, 4, 43, 15, 111, 103, -48, -50, -83, 52, 59, 103, 102, 83, -105, 87, 20, -120, 35, -7, -39, -24, 29, -39, -35, -87, 88, 120, 126, 19, 108, 34, -59, -20, 86, 47},
                        {19, -70, 36, 55, -42, -49, 33, 100, 105, -5, 89, 43, 3, -85, 60, -96, 43, -46, 86, -33, 120, -123, -99, -100, -34, 48, 82, -37, 34, 78, 127, 12, -39, -76, -26, 117, 74, -60, -68, -2, -37, -56, -6, 94, -27, 81, 32, -96, -19, -32, -77, 22, -56, -49, -38, -60, 45, -69, 40, 106, -106, -34, 101, -75},
                        {57, -92, -44, 8, -79, -88, -82, 58, -116, 93, 103, -127, 87, -121, -28, 31, -108, -14, -23, 38, 57, -83, -33, -110, 24, 6, 68, 124, -89, -35, -127, 5, -118, -78, -127, -35, 112, -34, 30, 24, -70, -71, 126, 39, -124, 122, -35, -97, -18, 25, 119, 79, 119, -65, 59, -20, -84, 120, -47, 4, -106, -125, -38, -113},
                        {18, -93, 34, -80, -43, 127, 57, -118, 24, -119, 25, 71, 59, -29, -108, -99, -122, 58, 44, 0, 42, -111, 25, 94, -36, 41, -64, -53, -78, -119, 85, 7, -45, -70, 81, -84, 71, -61, -68, -79, 112, 117, 19, 18, 70, 95, 108, -58, 48, 116, -89, 43, 66, 55, 37, -37, -60, 104, 47, -19, -56, 97, 73, 26},
                        {78, 4, -111, -36, 120, 111, -64, 46, 99, 125, -5, 97, -126, -21, 60, -78, -33, 89, 25, -60, 0, -49, 59, -118, 18, 3, -60, 30, 105, -92, -101, 15, 63, 50, 25, 2, -116, 78, -5, -25, -59, 74, -116, 64, -55, -121, 1, 69, 51, -119, 43, -6, -81, 14, 5, 84, -67, -73, 67, 24, 82, -37, 109, -93},
                        {-44, -30, -64, -68, -21, 74, 124, 122, 114, -89, -91, -51, 89, 32, 96, -1, -101, -112, -94, 98, -24, -31, -50, 100, -72, 56, 24, 30, 105, 115, 15, 3, -67, 107, -18, 111, -38, -93, -11, 24, 36, 73, -23, 108, 14, -41, -65, 32, 51, 22, 95, 41, 85, -121, -35, -107, 0, 105, -112, 59, 48, -22, -84, 46},
                        {4, 38, 54, -84, -78, 24, -48, 8, -117, 78, -95, 24, 25, -32, -61, 26, -97, -74, 46, -120, -125, 27, 73, 107, -17, -21, -6, -52, 47, -68, 66, 5, -62, -12, -102, -127, 48, -69, -91, -81, -33, -13, -9, -12, -44, -73, 40, -58, 120, -120, 108, 101, 18, -14, -17, -93, 113, 49, 76, -4, -113, -91, -93, -52},
                        {28, -48, 70, -35, 123, -31, 16, -52, 72, 84, -51, 78, 104, 59, -102, -112, 29, 28, 25, 66, 12, 75, 26, -85, 56, -12, -4, -92, 49, 86, -27, 12, 44, -63, 108, 82, -76, -97, -41, 95, -48, -95, -115, 1, 64, -49, -97, 90, 65, 46, -114, -127, -92, 79, 100, 49, 116, -58, -106, 9, 117, -7, -91, 96},
                        {58, 26, 18, 76, 127, -77, -58, -87, -116, -44, 60, -32, -4, -76, -124, 4, -60, 82, -5, -100, -95, 18, 2, -53, -50, -96, -126, 105, 93, 19, 74, 13, 87, 125, -72, -10, 42, 14, 91, 44, 78, 52, 60, -59, -27, -37, -57, 17, -85, 31, -46, 113, 100, -117, 15, 108, -42, 12, 47, 63, 1, 11, -122, -3}};

                for (int i = 0; i < recipients.length; i++) {

                    Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, 0, (short)0, CREATOR_PUBLIC_KEY, recipients[i], amounts[i], 0, 0, signatures[i]);

                    transactions.put(transaction.getId(), transaction);

                }

                for (Transaction transaction : transactions.values()) {
                    transaction.index = transactionCounter.incrementAndGet();
                    transaction.block = GENESIS_BLOCK_ID;

                }

                nxt.Transaction.saveTransactions("transactions.nxt");

            }

            try {

                Logger.logMessage("Loading blocks...");
                Block.loadBlocks("blocks.nxt");
                Logger.logMessage("...Done");

            } catch (FileNotFoundException e) {
                Logger.logMessage("blocks.nxt not found, starting from scratch");
                blocks.clear();

                Block block = new Block(-1, 0, 0, transactions.size(), 1000000000, 0, transactions.size() * 128, null, CREATOR_PUBLIC_KEY, new byte[64], new byte[]{105, -44, 38, -60, -104, -73, 10, -58, -47, 103, -127, -128, 53, 101, 39, -63, -2, -32, 48, -83, 115, 47, -65, 118, 114, -62, 38, 109, 22, 106, 76, 8, -49, -113, -34, -76, 82, 79, -47, -76, -106, -69, -54, -85, 3, -6, 110, 103, 118, 15, 109, -92, 82, 37, 20, 2, 36, -112, 21, 72, 108, 72, 114, 17});
                block.index = blockCounter.incrementAndGet();
                blocks.put(GENESIS_BLOCK_ID, block);

                int i = 0;
                for (long transaction : transactions.keySet()) {

                    block.transactions[i++] = transaction;

                }
                Arrays.sort(block.transactions);
                MessageDigest digest = Crypto.getMessageDigest("SHA-256");
                for (i = 0; i < block.transactions.length; i++) {
                    Transaction transaction = transactions.get(block.transactions[i]);
                    digest.update(transaction.getBytes());
                    block.blockTransactions[i] = transaction;
                }
                block.payloadHash = digest.digest();

                block.baseTarget = initialBaseTarget;
                block.cumulativeDifficulty = BigInteger.ZERO;
                lastBlock.set(block);

                Block.saveBlocks("blocks.nxt", false);

            }

            Logger.logMessage("Scanning blockchain...");
            Map<Long,Block> loadedBlocks = new HashMap<>(blocks);
            blocks.clear();
            long curBlockId = GENESIS_BLOCK_ID;
            Block curBlock;
            while ((curBlock = loadedBlocks.get(curBlockId)) != null) {
                curBlock.analyze();
                curBlockId = curBlock.nextBlock;
            }
            Logger.logMessage("...Done");

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    try {

                        if (Peer.getNumberOfConnectedPublicPeers() < Nxt.maxNumberOfConnectedPublicPeers) {

                            Peer peer = Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.STATE_NONCONNECTED : Peer.STATE_DISCONNECTED, false);
                            if (peer != null) {

                                peer.connect();

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error connecting to peer", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 5, TimeUnit.SECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    try {

                        long curTime = System.currentTimeMillis();

                        for (Peer peer : Nxt.peers.values()) {

                            if (peer.blacklistingTime > 0 && peer.blacklistingTime + Nxt.blacklistingPeriod <= curTime ) {

                                peer.removeBlacklistedStatus();

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error un-blacklisting peer", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 1, TimeUnit.SECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final JSONObject getPeersRequest = new JSONObject();
                {
                    getPeersRequest.put("requestType", "getPeers");
                }

                @Override
                public void run() {

                    try {

                        Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
                        if (peer != null) {
                            JSONObject response = peer.send(getPeersRequest);
                            if (response != null) {

                                JSONArray peers = (JSONArray)response.get("peers");
                                for (Object peerAddress : peers) {

                                    String address = ((String)peerAddress).trim();
                                    if (address.length() > 0) {
                                        //TODO: can a rogue peer fill the peer pool with zombie addresses?
                                        //consider an option to trust only highly-hallmarked peers
                                        Peer.addPeer(address, address);

                                    }

                                }

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error requesting peers from a peer", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 5, TimeUnit.SECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final JSONObject getUnconfirmedTransactionsRequest = new JSONObject();
                {
                    getUnconfirmedTransactionsRequest.put("requestType", "getUnconfirmedTransactions");
                }

                @Override
                public void run() {

                    try {

                        Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
                        if (peer != null) {

                            JSONObject response = peer.send(getUnconfirmedTransactionsRequest);
                            if (response != null) {

                                Transaction.processTransactions(response, "unconfirmedTransactions");

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error processing unconfirmed transactions from peer", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 5, TimeUnit.SECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    try {

                        int curTime = getEpochTime(System.currentTimeMillis());
                        JSONArray removedUnconfirmedTransactions = new JSONArray();

                        Iterator<Transaction> iterator = unconfirmedTransactions.values().iterator();
                        while (iterator.hasNext()) {

                            Transaction transaction = iterator.next();
                            if (transaction.timestamp + transaction.deadline * 60 < curTime || !transaction.validateAttachment()) {

                                iterator.remove();

                                Account account = accounts.get(transaction.getSenderAccountId());
                                account.addToUnconfirmedBalance((transaction.amount + transaction.fee) * 100L);

                                JSONObject removedUnconfirmedTransaction = new JSONObject();
                                removedUnconfirmedTransaction.put("index", transaction.index);
                                removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);

                            }

                        }

                        if (removedUnconfirmedTransactions.size() > 0) {

                            JSONObject response = new JSONObject();
                            response.put("response", "processNewData");

                            response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);

                            for (User user : users.values()) {

                                user.send(response);

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error removing unconfirmed transactions", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 1, TimeUnit.SECONDS);

            //TODO: figure out what this thread is actually doing
            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final JSONObject getCumulativeDifficultyRequest = new JSONObject();
                private final JSONObject getMilestoneBlockIdsRequest = new JSONObject();
                {
                    getCumulativeDifficultyRequest.put("requestType", "getCumulativeDifficulty");
                    getMilestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                }

                @Override
                public void run() {

                    try {

                        Peer peer = Peer.getAnyPeer(Peer.STATE_CONNECTED, true);
                        if (peer != null) {

                            lastBlockchainFeeder = peer;

                            JSONObject response = peer.send(getCumulativeDifficultyRequest);
                            if (response != null) {

                                BigInteger curCumulativeDifficulty = lastBlock.get().cumulativeDifficulty;
                                String peerCumulativeDifficulty = (String)response.get("cumulativeDifficulty");
                                if (peerCumulativeDifficulty == null) {
                                    return;
                                }
                                BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                                if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) > 0) {

                                    response = peer.send(getMilestoneBlockIdsRequest);
                                    if (response != null) {

                                        long commonBlockId = GENESIS_BLOCK_ID;

                                        JSONArray milestoneBlockIds = (JSONArray)response.get("milestoneBlockIds");
                                        for (Object milestoneBlockId : milestoneBlockIds) {

                                            long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                                            Block block = blocks.get(blockId);
                                            if (block != null) {

                                                commonBlockId = blockId;

                                                break;

                                            }

                                        }

                                        int i, numberOfBlocks;
                                        do {

                                            JSONObject request = new JSONObject();
                                            request.put("requestType", "getNextBlockIds");
                                            request.put("blockId", Convert.convert(commonBlockId));
                                            response = peer.send(request);
                                            if (response == null) {

                                                return;

                                            } else {

                                                JSONArray nextBlockIds = (JSONArray)response.get("nextBlockIds");
                                                numberOfBlocks = nextBlockIds.size();
                                                if (numberOfBlocks == 0) {

                                                    return;

                                                } else {

                                                    long blockId;
                                                    for (i = 0; i < numberOfBlocks; i++) {

                                                        blockId = Convert.parseUnsignedLong((String) nextBlockIds.get(i));
                                                        if (blocks.get(blockId) == null) {

                                                            break;

                                                        }

                                                        commonBlockId = blockId;

                                                    }

                                                }

                                            }

                                        } while (i == numberOfBlocks);

                                        if (lastBlock.get().height - blocks.get(commonBlockId).height < 720) {

                                            long curBlockId = commonBlockId;
                                            LinkedList<Block> futureBlocks = new LinkedList<>();
                                            HashMap<Long, Transaction> futureTransactions = new HashMap<>();

                                            do {

                                                JSONObject request = new JSONObject();
                                                request.put("requestType", "getNextBlocks");
                                                request.put("blockId", Convert.convert(curBlockId));
                                                response = peer.send(request);
                                                if (response == null) {

                                                    break;

                                                } else {

                                                    JSONArray nextBlocks = (JSONArray)response.get("nextBlocks");
                                                    numberOfBlocks = nextBlocks.size();
                                                    if (numberOfBlocks == 0) {

                                                        break;

                                                    } else {

                                                        synchronized (blocksAndTransactionsLock) {
                                                            for (i = 0; i < numberOfBlocks; i++) {

                                                                JSONObject blockData = (JSONObject)nextBlocks.get(i);
                                                                Block block = Block.getBlock(blockData);
                                                                if (block == null) {

                                                                    // peer tried to send us invalid transactions length or payload parameters
                                                                    peer.blacklist();
                                                                    return;

                                                                }

                                                                curBlockId = block.getId();

                                                                //synchronized (blocksAndTransactionsLock) {

                                                                boolean alreadyPushed = false;
                                                                if (block.previousBlock == lastBlock.get().getId()) {

                                                                    ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + block.payloadLength);
                                                                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                                    buffer.put(block.getBytes());

                                                                    JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                                                                    for (Object transaction : transactionsData) {

                                                                        buffer.put(Transaction.getTransaction((JSONObject)transaction).getBytes());

                                                                    }

                                                                    if (Block.pushBlock(buffer, false)) {

                                                                        alreadyPushed = true;

                                                                    } else {

                                                                        peer.blacklist();

                                                                        return;

                                                                    }

                                                                }
                                                                if (!alreadyPushed && blocks.get(block.getId()) == null && block.transactions.length <= MAX_NUMBER_OF_TRANSACTIONS) {

                                                                    futureBlocks.add(block);

                                                                    JSONArray transactionsData = (JSONArray)blockData.get("transactions");
                                                                    for (int j = 0; j < block.transactions.length; j++) {

                                                                        Transaction transaction = Transaction.getTransaction((JSONObject)transactionsData.get(j));
                                                                        block.transactions[j] = transaction.getId();
                                                                        block.blockTransactions[j] = transaction;
                                                                        futureTransactions.put(block.transactions[j], transaction);

                                                                    }

                                                                }

                                                                //}

                                                            }

                                                        } //synchronized
                                                    }

                                                }

                                            } while (true);

                                            if (!futureBlocks.isEmpty() && lastBlock.get().height - blocks.get(commonBlockId).height < 720) {

                                                synchronized (blocksAndTransactionsLock) {

                                                    Block.saveBlocks("blocks.nxt.bak", true);
                                                    Transaction.saveTransactions("transactions.nxt.bak");

                                                    curCumulativeDifficulty = lastBlock.get().cumulativeDifficulty;

                                                    while (lastBlock.get().getId() != commonBlockId && Block.popLastBlock()) {}

                                                    if (lastBlock.get().getId() == commonBlockId) {

                                                        for (Block block : futureBlocks) {

                                                            if (block.previousBlock == lastBlock.get().getId()) {

                                                                ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + block.payloadLength);
                                                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                                buffer.put(block.getBytes());

                                                                for (Transaction transaction : block.blockTransactions) {

                                                                    buffer.put(transaction.getBytes());

                                                                }

                                                                if (!Block.pushBlock(buffer, false)) {

                                                                    break;

                                                                }

                                                            }

                                                        }

                                                    }

                                                    if (lastBlock.get().cumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {

                                                        Block.loadBlocks("blocks.nxt.bak");
                                                        Transaction.loadTransactions("transactions.nxt.bak");

                                                        peer.blacklist();

                                                        Nxt.accounts.clear();
                                                        Nxt.aliases.clear();
                                                        Nxt.aliasIdToAliasMappings.clear();
                                                        Nxt.unconfirmedTransactions.clear();
                                                        Nxt.doubleSpendingTransactions.clear();
                                                        //TODO: clean this up
                                                        Logger.logMessage("Re-scanning blockchain...");
                                                        Map<Long,Block> loadedBlocks = new HashMap<>(blocks);
                                                        blocks.clear();
                                                        long currentBlockId = GENESIS_BLOCK_ID;
                                                        Block currentBlock;
                                                        while ((currentBlock = loadedBlocks.get(currentBlockId)) != null) {
                                                            currentBlock.analyze();
                                                            currentBlockId = currentBlock.nextBlock;
                                                        }
                                                        Logger.logMessage("...Done");


                                                    }

                                                }

                                            }

                                            synchronized (blocksAndTransactionsLock) {
                                                Block.saveBlocks("blocks.nxt", false);
                                                Transaction.saveTransactions("transactions.nxt");
                                            }

                                        }

                                    }

                                }

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error in milestone blocks processing thread", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 1, TimeUnit.SECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap<>();
                private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap<>();


                @Override
                public void run() {

                    try {

                        HashMap<Account, User> unlockedAccounts = new HashMap<>();
                        for (User user : users.values()) {

                            if (user.secretPhrase != null) {

                                Account account = accounts.get(Account.getId(user.publicKey));
                                if (account != null && account.getEffectiveBalance() > 0) {

                                    unlockedAccounts.put(account, user);

                                }

                            }

                        }

                        for (Map.Entry<Account, User> unlockedAccountEntry : unlockedAccounts.entrySet()) {

                            Account account = unlockedAccountEntry.getKey();
                            User user = unlockedAccountEntry.getValue();
                            Block lastBlock = Nxt.lastBlock.get();
                            if (lastBlocks.get(account) != lastBlock) {

                                long effectiveBalance = account.getEffectiveBalance();
                                if (effectiveBalance <= 0) {
                                    continue;
                                }
                                MessageDigest digest = Crypto.getMessageDigest("SHA-256");
                                byte[] generationSignatureHash;
                                if (lastBlock.height < TRANSPARENT_FORGING_BLOCK) {

                                    byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
                                    generationSignatureHash = digest.digest(generationSignature);

                                } else {

                                    digest.update(lastBlock.generationSignature);
                                    generationSignatureHash = digest.digest(user.publicKey);

                                }
                                BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

                                lastBlocks.put(account, lastBlock);
                                hits.put(account, hit);

                                JSONObject response = new JSONObject();
                                response.put("response", "setBlockGenerationDeadline");
                                response.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp));

                                user.send(response);

                            }

                            int elapsedTime = getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp;
                            if (elapsedTime > 0) {

                                BigInteger target = BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(account.getEffectiveBalance())).multiply(BigInteger.valueOf(elapsedTime));
                                if (hits.get(account).compareTo(target) < 0) {

                                    account.generateBlock(user.secretPhrase);

                                }

                            }

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error in block generation thread", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 1, TimeUnit.SECONDS);

            scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    try {

                        JSONArray transactionsData = new JSONArray();

                        for (Transaction transaction : nonBroadcastedTransactions.values()) {

                            if (unconfirmedTransactions.get(transaction.id) == null && transactions.get(transaction.id) == null) {

                                transactionsData.add(transaction.getJSONObject());

                            } else {

                                nonBroadcastedTransactions.remove(transaction.id);

                            }

                        }

                        if (transactionsData.size() > 0) {

                            JSONObject peerRequest = new JSONObject();
                            peerRequest.put("requestType", "processTransactions");
                            peerRequest.put("transactions", transactionsData);

                            Peer.sendToSomePeers(peerRequest);

                        }

                    } catch (Exception e) {
                        Logger.logDebugMessage("Error in transaction re-broadcasting thread", e);
                    } catch (Throwable t) {
                        Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                        t.printStackTrace();
                        System.exit(1);
                    }

                }

            }, 0, 60, TimeUnit.SECONDS);

            Logger.logMessage("NRS " + Nxt.VERSION + " started successfully.");

        } catch (Exception e) {

            Logger.logMessage("Error initializing Nxt servlet", e);
            System.exit(1);

        }

    }

    //TODO: clean up Exception and error handling as part of the refactoring
    //TODO: the huge switch statement should be refactored completely, a separate class should handle each case
    // This is required in order to be able to factor out closed-source code into separate class files
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        User user = null;

        try {

            String userPasscode = req.getParameter("user");
            if (userPasscode == null) {

                JSONObject response = new JSONObject();

                if (allowedBotHosts != null && !allowedBotHosts.contains(req.getRemoteHost())) {

                    response.put("errorCode", 7);
                    response.put("errorDescription", "Not allowed");

                } else {

                    String requestType = req.getParameter("requestType");
                    if (requestType == null) {

                        response.put("errorCode", 1);
                        response.put("errorDescription", "Incorrect request");

                    } else {

                        switch (requestType) {

                            case "assignAlias":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String alias = req.getParameter("alias");
                                String uri = req.getParameter("uri");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (alias == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"alias\" not specified");

                                } else if (uri == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"uri\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    alias = alias.trim();
                                    if (alias.length() == 0 || alias.length() > 100) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"alias\" (length must be in [1..100] range)");

                                    } else {

                                        String normalizedAlias = alias.toLowerCase();
                                        int i;
                                        for (i = 0; i < normalizedAlias.length(); i++) {

                                            if (Convert.alphabet.indexOf(normalizedAlias.charAt(i)) < 0) {

                                                break;

                                            }

                                        }
                                        if (i != normalizedAlias.length()) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"alias\" (must contain only digits and latin letters)");

                                        } else {

                                            uri = uri.trim();
                                            if (uri.length() > 1000) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"uri\" (length must be not longer than 1000 characters)");

                                            } else {

                                                try {

                                                    int fee = Integer.parseInt(feeValue);
                                                    if (fee <= 0 || fee >= MAX_BALANCE) {

                                                        throw new Exception();

                                                    }

                                                    try {

                                                        short deadline = Short.parseShort(deadlineValue);
                                                        if (deadline < 1) {

                                                            throw new Exception();
                                                            //TODO: better error checking
                                                            // cfb: This is a part of Legacy API, it isn't worth rewriting

                                                        }

                                                        long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                                        long accountId = Account.getId(publicKey);
                                                        Account account = accounts.get(accountId);
                                                        if (account == null) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            if (fee * 100L > account.getUnconfirmedBalance()) {

                                                                response.put("errorCode", 6);
                                                                response.put("errorDescription", "Not enough funds");

                                                            } else {

                                                                Alias aliasData = aliases.get(normalizedAlias);
                                                                if (aliasData != null && aliasData.account != account) {

                                                                    response.put("errorCode", 8);
                                                                    response.put("errorDescription", "\"" + alias + "\" is already used");

                                                                } else {

                                                                    int timestamp = getEpochTime(System.currentTimeMillis());

                                                                    Transaction transaction = new Transaction(Transaction.TYPE_MESSAGING, Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
                                                                    transaction.attachment = new Attachment.MessagingAliasAssignment(alias, uri);
                                                                    transaction.sign(secretPhrase);

                                                                    JSONObject peerRequest = new JSONObject();
                                                                    peerRequest.put("requestType", "processTransactions");
                                                                    JSONArray transactionsData = new JSONArray();
                                                                    transactionsData.add(transaction.getJSONObject());
                                                                    peerRequest.put("transactions", transactionsData);

                                                                    Peer.sendToSomePeers(peerRequest);

                                                                    response.put("transaction", transaction.getStringId());

                                                                    nonBroadcastedTransactions.put(transaction.id, transaction);

                                                                }

                                                            }

                                                        }

                                                    } catch (Exception e) {

                                                        response.put("errorCode", 4);
                                                        response.put("errorDescription", "Incorrect \"deadline\"");

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"fee\"");

                                                }

                                            }

                                        }

                                    }

                                }

                            }
                            break;

                            case "broadcastTransaction":
                            {

                                String transactionBytes = req.getParameter("transactionBytes");
                                if (transactionBytes == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"transactionBytes\" not specified");

                                } else {

                                    try {

                                        ByteBuffer buffer = ByteBuffer.wrap(Convert.convert(transactionBytes));
                                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                                        Transaction transaction = Transaction.getTransaction(buffer);

                                        JSONObject peerRequest = new JSONObject();
                                        peerRequest.put("requestType", "processTransactions");
                                        JSONArray transactionsData = new JSONArray();
                                        transactionsData.add(transaction.getJSONObject());
                                        peerRequest.put("transactions", transactionsData);

                                        Peer.sendToSomePeers(peerRequest);

                                        response.put("transaction", transaction.getStringId());

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"transactionBytes\"");

                                    }

                                }

                            }
                            break;

                            case "decodeHallmark":
                            {

                                String hallmarkValue = req.getParameter("hallmark");
                                if (hallmarkValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"hallmark\" not specified");

                                } else {

                                    try {

                                        byte[] hallmark = Convert.convert(hallmarkValue);

                                        ByteBuffer buffer = ByteBuffer.wrap(hallmark);
                                        buffer.order(ByteOrder.LITTLE_ENDIAN);

                                        byte[] publicKey = new byte[32];
                                        buffer.get(publicKey);
                                        int hostLength = buffer.getShort();
                                        byte[] hostBytes = new byte[hostLength];
                                        buffer.get(hostBytes);
                                        String host = new String(hostBytes, "UTF-8");
                                        int weight = buffer.getInt();
                                        int date = buffer.getInt();
                                        buffer.get();
                                        byte[] signature = new byte[64];
                                        buffer.get(signature);

                                        response.put("account", Convert.convert(Account.getId(publicKey)));
                                        response.put("host", host);
                                        response.put("weight", weight);
                                        int year = date / 10000;
                                        int month = (date % 10000) / 100;
                                        int day = date % 100;
                                        response.put("date", (year < 10 ? "000" : (year < 100 ? "00" : (year < 1000 ? "0" : ""))) + year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day);
                                        byte[] data = new byte[hallmark.length - 64];
                                        System.arraycopy(hallmark, 0, data, 0, data.length);
                                        response.put("valid", host.length() > 100 || weight <= 0 || weight > MAX_BALANCE ? false : Crypto.verify(signature, data, publicKey));

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"hallmark\"");

                                    }

                                }

                            }
                            break;

                            case "decodeToken":
                            {

                                String website = req.getParameter("website");
                                String token = req.getParameter("token");
                                if (website == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"website\" not specified");

                                } else if (token == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"token\" not specified");

                                } else {

                                    byte[] websiteBytes = website.trim().getBytes("UTF-8");
                                    byte[] tokenBytes = new byte[100];
                                    int i = 0, j = 0;
                                    try {

                                        for (; i < token.length(); i += 8, j += 5) {

                                            long number = Long.parseLong(token.substring(i, i + 8), 32);
                                            tokenBytes[j] = (byte)number;
                                            tokenBytes[j + 1] = (byte)(number >> 8);
                                            tokenBytes[j + 2] = (byte)(number >> 16);
                                            tokenBytes[j + 3] = (byte)(number >> 24);
                                            tokenBytes[j + 4] = (byte)(number >> 32);

                                        }

                                    } catch (Exception e) { }

                                    if (i != 160) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"token\"");

                                    } else {

                                        byte[] publicKey = new byte[32];
                                        System.arraycopy(tokenBytes, 0, publicKey, 0, 32);
                                        int timestamp = (tokenBytes[32] & 0xFF) | ((tokenBytes[33] & 0xFF) << 8) | ((tokenBytes[34] & 0xFF) << 16) | ((tokenBytes[35] & 0xFF) << 24);
                                        byte[] signature = new byte[64];
                                        System.arraycopy(tokenBytes, 36, signature, 0, 64);

                                        byte[] data = new byte[websiteBytes.length + 36];
                                        System.arraycopy(websiteBytes, 0, data, 0, websiteBytes.length);
                                        System.arraycopy(tokenBytes, 0, data, websiteBytes.length, 36);
                                        boolean valid = Crypto.verify(signature, data, publicKey);

                                        response.put("account", Convert.convert(Account.getId(publicKey)));
                                        response.put("timestamp", timestamp);
                                        response.put("valid", valid);

                                    }

                                }

                            }
                            break;

                            case "getAccountBlockIds":
                            {

                                String account = req.getParameter("account");
                                String timestampValue = req.getParameter("timestamp");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else if (timestampValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"timestamp\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown account");

                                        } else {

                                            try {

                                                int timestamp = Integer.parseInt(timestampValue);
                                                if (timestamp < 0) {

                                                    throw new Exception();

                                                }

                                                PriorityQueue<Block> sortedBlocks = new PriorityQueue<>(11, Block.heightComparator);
                                                byte[] accountPublicKey = accountData.publicKey.get();
                                                for (Block block : blocks.values()) {

                                                    if (block.timestamp >= timestamp && Arrays.equals(block.generatorPublicKey, accountPublicKey)) {

                                                        sortedBlocks.offer(block);

                                                    }

                                                }
                                                JSONArray blockIds = new JSONArray();
                                                while (! sortedBlocks.isEmpty()) {
                                                    blockIds.add(sortedBlocks.poll().getStringId());
                                                }
                                                response.put("blockIds", blockIds);

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"timestamp\"");

                                            }

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getAccountId":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else {

                                    byte[] publicKeyHash = Crypto.getMessageDigest("SHA-256").digest(Crypto.getPublicKey(secretPhrase));
                                    BigInteger bigInteger = new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
                                    response.put("accountId", bigInteger.toString());

                                }

                            }
                            break;

                            case "getAccountPublicKey":
                            {

                                String account = req.getParameter("account");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown account");

                                        } else {

                                            if (accountData.publicKey.get() != null) {

                                                response.put("publicKey", Convert.convert(accountData.publicKey.get()));

                                            }

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getAccountTransactionIds":
                            {

                                String account = req.getParameter("account");
                                String timestampValue = req.getParameter("timestamp");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else if (timestampValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"timestamp\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown account");

                                        } else {

                                            try {

                                                int timestamp = Integer.parseInt(timestampValue);
                                                if (timestamp < 0) {

                                                    throw new Exception();

                                                }

                                                int type, subtype;
                                                try {

                                                    type = Integer.parseInt(req.getParameter("type"));

                                                } catch (Exception e) {

                                                    type = -1;

                                                }
                                                try {

                                                    subtype = Integer.parseInt(req.getParameter("subtype"));

                                                } catch (Exception e) {

                                                    subtype = -1;

                                                }

                                                PriorityQueue<Transaction> sortedTransactions = new PriorityQueue<>(11, Transaction.timestampComparator);
                                                byte[] accountPublicKey = accountData.publicKey.get();
                                                for (Transaction transaction : transactions.values()) {
                                                    if ((transaction.recipient == accountData.id || Arrays.equals(transaction.senderPublicKey, accountPublicKey)) && (type < 0 || transaction.type == type) && (subtype < 0 || transaction.subtype == subtype) && blocks.get(transaction.block).timestamp >= timestamp) {
                                                        sortedTransactions.offer(transaction);
                                                    }
                                                }
                                                JSONArray transactionIds = new JSONArray();
                                                while (! sortedTransactions.isEmpty()) {
                                                    transactionIds.add(sortedTransactions.poll().getStringId());
                                                }
                                                response.put("transactionIds", transactionIds);

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"timestamp\"");

                                            }

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getAlias":
                            {

                                String alias = req.getParameter("alias");
                                if (alias == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"alias\" not specified");

                                } else {

                                    try {

                                        Alias aliasData = aliasIdToAliasMappings.get(Convert.parseUnsignedLong(alias));
                                        if (aliasData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown alias");

                                        } else {

                                            response.put("account", Convert.convert(aliasData.account.id));
                                            response.put("alias", aliasData.alias);
                                            if (aliasData.uri.length() > 0) {

                                                response.put("uri", aliasData.uri);

                                            }
                                            response.put("timestamp", aliasData.timestamp);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"alias\"");

                                    }

                                }

                            }
                            break;

                            case "getAliasId":
                            {

                                String alias = req.getParameter("alias");
                                if (alias == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"alias\" not specified");

                                } else {

                                    Alias aliasData = aliases.get(alias.toLowerCase());
                                    if (aliasData == null) {

                                        response.put("errorCode", 5);
                                        response.put("errorDescription", "Unknown alias");

                                    } else {

                                        response.put("id", Convert.convert(aliasData.id));

                                    }

                                }

                            }
                            break;

                            case "getAliasIds":
                            {

                                String timestampValue = req.getParameter("timestamp");
                                if (timestampValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"timestamp\" not specified");

                                } else {

                                    try {

                                        int timestamp = Integer.parseInt(timestampValue);
                                        if (timestamp < 0) {

                                            throw new Exception();

                                        }

                                        JSONArray aliasIds = new JSONArray();
                                        for (Map.Entry<Long, Alias> aliasEntry : aliasIdToAliasMappings.entrySet()) {

                                            if (aliasEntry.getValue().timestamp >= timestamp) {

                                                aliasIds.add(Convert.convert(aliasEntry.getKey()));

                                            }

                                        }
                                        response.put("aliasIds", aliasIds);

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"timestamp\"");

                                    }

                                }

                            }
                            break;

                            case "getAliasURI":
                            {

                                String alias = req.getParameter("alias");
                                if (alias == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"alias\" not specified");

                                } else {

                                    Alias aliasData = aliases.get(alias.toLowerCase());
                                    if (aliasData == null) {

                                        response.put("errorCode", 5);
                                        response.put("errorDescription", "Unknown alias");

                                    } else {

                                        if (aliasData.uri.length() > 0) {

                                            response.put("uri", aliasData.uri);

                                        }

                                    }

                                }

                                /*String origin = req.getHeader("Origin");
                                if (origin != null) {

                                    resp.setHeader("Access-Control-Allow-Origin", origin);

                                }*/

                            }
                            break;

                            case "getBalance":
                            {

                                String account = req.getParameter("account");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("balance", 0);
                                            response.put("unconfirmedBalance", 0);
                                            response.put("effectiveBalance", 0);

                                        } else {

                                            synchronized (accountData) {

                                                response.put("balance", accountData.getBalance());
                                                response.put("unconfirmedBalance", accountData.getUnconfirmedBalance());
                                                response.put("effectiveBalance", accountData.getEffectiveBalance() * 100L);

                                            }

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getBlock":
                            {

                                String block = req.getParameter("block");
                                if (block == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"block\" not specified");

                                } else {

                                    try {

                                        Block blockData = blocks.get(Convert.parseUnsignedLong(block));
                                        if (blockData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown block");

                                        } else {

                                            response.put("height", blockData.height);
                                            response.put("generator", Convert.convert(blockData.getGeneratorAccountId()));
                                            response.put("timestamp", blockData.timestamp);
                                            response.put("numberOfTransactions", blockData.transactions.length);
                                            response.put("totalAmount", blockData.totalAmount);
                                            response.put("totalFee", blockData.totalFee);
                                            response.put("payloadLength", blockData.payloadLength);
                                            response.put("version", blockData.version);
                                            response.put("baseTarget", Convert.convert(blockData.baseTarget));
                                            if (blockData.previousBlock != 0) {

                                                response.put("previousBlock", Convert.convert(blockData.previousBlock));

                                            }
                                            if (blockData.nextBlock != 0) {

                                                response.put("nextBlock", Convert.convert(blockData.nextBlock));

                                            }
                                            response.put("payloadHash", Convert.convert(blockData.payloadHash));
                                            response.put("generationSignature", Convert.convert(blockData.generationSignature));
                                            if (blockData.version > 1) {

                                                response.put("previousBlockHash", Convert.convert(blockData.previousBlockHash));

                                            }
                                            response.put("blockSignature", Convert.convert(blockData.blockSignature));
                                            JSONArray transactions = new JSONArray();
                                            for (long transactionId : blockData.transactions) {

                                                transactions.add(Convert.convert(transactionId));

                                            }
                                            response.put("transactions", transactions);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"block\"");

                                    }

                                }

                            }
                            break;

                            //TODO: cache
                            case "getConstants":
                            {

                                response.put("genesisBlockId", Convert.convert(GENESIS_BLOCK_ID));
                                response.put("genesisAccountId", Convert.convert(CREATOR_ID));
                                response.put("maxBlockPayloadLength", MAX_PAYLOAD_LENGTH);
                                response.put("maxArbitraryMessageLength", MAX_ARBITRARY_MESSAGE_LENGTH);

                                JSONArray transactionTypes = new JSONArray();
                                JSONObject transactionType = new JSONObject();
                                transactionType.put("value", Transaction.TYPE_PAYMENT);
                                transactionType.put("description", "Payment");
                                JSONArray subtypes = new JSONArray();
                                JSONObject subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT);
                                subtype.put("description", "Ordinary payment");
                                subtypes.add(subtype);
                                transactionType.put("subtypes", subtypes);
                                transactionTypes.add(transactionType);
                                transactionType = new JSONObject();
                                transactionType.put("value", Transaction.TYPE_MESSAGING);
                                transactionType.put("description", "Messaging");
                                subtypes = new JSONArray();
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE);
                                subtype.put("description", "Arbitrary message");
                                subtypes.add(subtype);
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT);
                                subtype.put("description", "Alias assignment");
                                subtypes.add(subtype);
                                transactionType.put("subtypes", subtypes);
                                transactionTypes.add(transactionType);
                                transactionType = new JSONObject();
                                transactionType.put("value", Transaction.TYPE_COLORED_COINS);
                                transactionType.put("description", "Colored coins");
                                subtypes = new JSONArray();
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE);
                                subtype.put("description", "Asset issuance");
                                subtypes.add(subtype);
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER);
                                subtype.put("description", "Asset transfer");
                                subtypes.add(subtype);
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT);
                                subtype.put("description", "Ask order placement");
                                subtypes.add(subtype);
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT);
                                subtype.put("description", "Bid order placement");
                                subtypes.add(subtype);
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION);
                                subtype.put("description", "Ask order cancellation");
                                subtypes.add(subtype);
                                subtype = new JSONObject();
                                subtype.put("value", Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION);
                                subtype.put("description", "Bid order cancellation");
                                subtypes.add(subtype);
                                transactionType.put("subtypes", subtypes);
                                transactionTypes.add(transactionType);
                                response.put("transactionTypes", transactionTypes);

                                JSONArray peerStates = new JSONArray();
                                JSONObject peerState = new JSONObject();
                                peerState.put("value", 0);
                                peerState.put("description", "Non-connected");
                                peerStates.add(peerState);
                                peerState = new JSONObject();
                                peerState.put("value", 1);
                                peerState.put("description", "Connected");
                                peerStates.add(peerState);
                                peerState = new JSONObject();
                                peerState.put("value", 2);
                                peerState.put("description", "Disconnected");
                                peerStates.add(peerState);
                                response.put("peerStates", peerStates);

                            }
                            break;

                            case "getGuaranteedBalance":
                            {

                                String account = req.getParameter("account");
                                String numberOfConfirmationsValue = req.getParameter("numberOfConfirmations");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else if (numberOfConfirmationsValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"numberOfConfirmations\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("guaranteedBalance", 0);

                                        } else {

                                            try {

                                                int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                                                response.put("guaranteedBalance", accountData.getGuaranteedBalance(numberOfConfirmations));

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"numberOfConfirmations\"");

                                            }

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getMyInfo":
                            {

                                response.put("host", req.getRemoteHost());
                                response.put("address", req.getRemoteAddr());

                            }
                            break;

                            case "getPeer":
                            {

                                String peer = req.getParameter("peer");
                                if (peer == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"peer\" not specified");

                                } else {

                                    Peer peerData = peers.get(peer);
                                    if (peerData == null) {

                                        response.put("errorCode", 5);
                                        response.put("errorDescription", "Unknown peer");

                                    } else {

                                        response.put("state", peerData.state);
                                        response.put("announcedAddress", peerData.announcedAddress);
                                        if (peerData.hallmark != null) {

                                            response.put("hallmark", peerData.hallmark);

                                        }
                                        response.put("weight", peerData.getWeight());
                                        response.put("downloadedVolume", peerData.downloadedVolume);
                                        response.put("uploadedVolume", peerData.uploadedVolume);
                                        response.put("application", peerData.application);
                                        response.put("version", peerData.version);
                                        response.put("platform", peerData.platform);

                                    }

                                }

                            }
                            break;

                            case "getPeers":
                            {

                                JSONArray peers = new JSONArray();
                                peers.addAll(Nxt.peers.keySet());
                                response.put("peers", peers);

                            }
                            break;

                            case "getState":
                            {

                                response.put("version", VERSION);
                                response.put("time", getEpochTime(System.currentTimeMillis()));
                                response.put("lastBlock", lastBlock.get().getStringId());
                                response.put("cumulativeDifficulty", lastBlock.get().cumulativeDifficulty.toString());

                                long totalEffectiveBalance = 0;
                                for (Account account : accounts.values()) {

                                    long effectiveBalance = account.getEffectiveBalance();
                                    if (effectiveBalance > 0) {

                                        totalEffectiveBalance += effectiveBalance;

                                    }

                                }
                                response.put("totalEffectiveBalance", totalEffectiveBalance * 100L);

                                response.put("numberOfBlocks", blocks.size());
                                response.put("numberOfTransactions", transactions.size());
                                response.put("numberOfAccounts", accounts.size());
                                response.put("numberOfAssets", assets.size());
                                response.put("numberOfOrders", askOrders.size() + bidOrders.size());
                                response.put("numberOfAliases", aliases.size());
                                response.put("numberOfPeers", peers.size());
                                response.put("numberOfUsers", users.size());
                                response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.announcedAddress);
                                response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
                                response.put("maxMemory", Runtime.getRuntime().maxMemory());
                                response.put("totalMemory", Runtime.getRuntime().totalMemory());
                                response.put("freeMemory", Runtime.getRuntime().freeMemory());

                            }
                            break;

                            case "getTime":
                            {

                                response.put("time", getEpochTime(System.currentTimeMillis()));

                            }
                            break;

                            case "getTransaction":
                            {

                                String transaction = req.getParameter("transaction");
                                if (transaction == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"transaction\" not specified");

                                } else {

                                    try {

                                        long transactionId = Convert.parseUnsignedLong(transaction);
                                        Transaction transactionData = transactions.get(transactionId);
                                        if (transactionData == null) {

                                            transactionData = unconfirmedTransactions.get(transactionId);
                                            if (transactionData == null) {

                                                response.put("errorCode", 5);
                                                response.put("errorDescription", "Unknown transaction");

                                            } else {

                                                response = transactionData.getJSONObject();
                                                response.put("sender", Convert.convert(transactionData.getSenderAccountId()));

                                            }

                                        } else {

                                            response = transactionData.getJSONObject();

                                            response.put("sender", Convert.convert(transactionData.getSenderAccountId()));
                                            Block block = blocks.get(transactionData.block);
                                            response.put("block", block.getStringId());
                                            response.put("confirmations", lastBlock.get().height - block.height + 1);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"transaction\"");

                                    }

                                }

                            }
                            break;

                            case "getTransactionBytes":
                            {

                                String transaction = req.getParameter("transaction");
                                if (transaction == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"transaction\" not specified");

                                } else {

                                    try {

                                        long transactionId = Convert.parseUnsignedLong(transaction);
                                        Transaction transactionData = transactions.get(transactionId);
                                        if (transactionData == null) {

                                            transactionData = unconfirmedTransactions.get(transactionId);
                                            if (transactionData == null) {

                                                response.put("errorCode", 5);
                                                response.put("errorDescription", "Unknown transaction");

                                            } else {

                                                response.put("bytes", Convert.convert(transactionData.getBytes()));

                                            }

                                        } else {

                                            response.put("bytes", Convert.convert(transactionData.getBytes()));
                                            Block block = blocks.get(transactionData.block);
                                            response.put("confirmations", lastBlock.get().height - block.height + 1);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"transaction\"");

                                    }

                                }

                            }
                            break;

                            case "getUnconfirmedTransactionIds":
                            {

                                JSONArray transactionIds = new JSONArray();
                                for (Transaction transaction : unconfirmedTransactions.values()) {

                                    transactionIds.add(transaction.getStringId());

                                }
                                response.put("unconfirmedTransactionIds", transactionIds);

                            }
                            break;

                            //TODO: uncomment, review and clean up code, comment out again

                        case "issueAsset":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String name = req.getParameter("name");
                                String description = req.getParameter("description");
                                String quantityValue = req.getParameter("quantity");
                                String feeValue = req.getParameter("fee");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (name == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"name\" not specified");

                                } else if (quantityValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"quantity\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else {

                                    name = name.trim();
                                    if (name.length() < 3 || name.length() > 10) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"name\" (length must be in [3..10] range)");

                                    } else {

                                        String normalizedName = name.toLowerCase();
                                        int i;
                                        for (i = 0; i < normalizedName.length(); i++) {

                                            if (Convert.alphabet.indexOf(normalizedName.charAt(i)) < 0) {

                                                break;

                                            }

                                        }
                                        if (i != normalizedName.length()) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"name\" (must contain only digits and latin letters)");

                                        } else if (assetNameToIdMappings.get(normalizedName) != null) {

                                            response.put("errorCode", 8);
                                            response.put("errorDescription", "\"" + name + "\" is already used");

                                        } else {

                                            if (description != null && description.length() > 1000) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"description\" (length must be not longer than 1000 characters)");

                                            } else {

                                                try {

                                                    int quantity = Integer.parseInt(quantityValue);
                                                    if (quantity <= 0 || quantity > MAX_ASSET_QUANTITY) {

                                                        response.put("errorCode", 4);
                                                        response.put("errorDescription", "Incorrect \"quantity\" (must be in [1..1'000'000'000] range)");

                                                    } else {

                                                        try {

                                                            int fee = Integer.parseInt(feeValue);
                                                            if (fee < Transaction.ASSET_ISSUANCE_FEE) {

                                                                response.put("errorCode", 4);
                                                                response.put("errorDescription", "Incorrect \"fee\" (must be not less than 1'000)");

                                                            } else {

                                                                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                                                Account account = accounts.get(Account.getId(publicKey));
                                                                if (account == null) {

                                                                    response.put("errorCode", 6);
                                                                    response.put("errorDescription", "Not enough funds");

                                                                } else {

                                                                    if (fee * 100L > account.getUnconfirmedBalance()) {

                                                                        response.put("errorCode", 6);
                                                                        response.put("errorDescription", "Not enough funds");

                                                                    } else {

                                                                        int timestamp = getEpochTime(System.currentTimeMillis());

                                                                        Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE, timestamp, (short)1440, publicKey, CREATOR_ID, 0, fee, 0, new byte[64]);
                                                                        transaction.attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantity);
                                                                        transaction.sign(secretPhrase);

                                                                        JSONObject peerRequest = new JSONObject();
                                                                        peerRequest.put("requestType", "processTransactions");
                                                                        JSONArray transactionsData = new JSONArray();
                                                                        transactionsData.add(transaction.getJSONObject());
                                                                        peerRequest.put("transactions", transactionsData);

                                                                        Peer.sendToSomePeers(peerRequest);

                                                                        response.put("transaction", transaction.getStringId());

                                                                        nonBroadcastedTransactions.put(transaction.id, transaction);

                                                                    }

                                                                }

                                                            }

                                                        } catch (Exception e) {

                                                            response.put("errorCode", 4);
                                                            response.put("errorDescription", "Incorrect \"fee\""+e.toString());

                                                        }

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"quantity\"");

                                                }

                                            }

                                        }

                                    }

                                }

                            }
                            break;

                        case "cancelAskOrder":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String orderValue = req.getParameter("order");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (orderValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"order\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    try {

                                        long order = Convert.parseUnsignedLong(orderValue);

                                        try {

                                            int fee = Integer.parseInt(feeValue);
                                            if (fee <= 0 || fee >= MAX_BALANCE) {

                                                throw new Exception();

                                            }

                                            try {

                                                short deadline = Short.parseShort(deadlineValue);
                                                if (deadline < 1) {

                                                    throw new Exception();

                                                }

                                                long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                                long accountId = Account.getId(publicKey);

                                                AskOrder orderData = askOrders.get(order);
                                                if (orderData == null || orderData.account.id != accountId) {

                                                    response.put("errorCode", 5);
                                                    response.put("errorDescription", "Unknown order");

                                                } else {

                                                    Account account = accounts.get(accountId);
                                                    if (account == null) {

                                                        response.put("errorCode", 6);
                                                        response.put("errorDescription", "Not enough funds");

                                                    } else {

                                                        if (fee * 100L > account.getUnconfirmedBalance()) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            int timestamp = getEpochTime(System.currentTimeMillis());

                                                            Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
                                                            transaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(order);
                                                            transaction.sign(secretPhrase);

                                                            JSONObject peerRequest = new JSONObject();
                                                            peerRequest.put("requestType", "processTransactions");
                                                            JSONArray transactionsData = new JSONArray();
                                                            transactionsData.add(transaction.getJSONObject());
                                                            peerRequest.put("transactions", transactionsData);

                                                            Peer.sendToSomePeers(peerRequest);

                                                            response.put("transaction", transaction.getStringId());

                                                            nonBroadcastedTransactions.put(transaction.id, transaction);

                                                        }

                                                    }

                                                }

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"deadline\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"fee\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"order\"");

                                    }

                                }

                            }
                            break;

                        case "cancelBidOrder":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String orderValue = req.getParameter("order");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (orderValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"order\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    try {

                                        long order = Convert.parseUnsignedLong(orderValue);

                                        try {

                                            int fee = Integer.parseInt(feeValue);
                                            if (fee <= 0 || fee >= MAX_BALANCE) {

                                                throw new Exception();

                                            }

                                            try {

                                                short deadline = Short.parseShort(deadlineValue);
                                                if (deadline < 1) {

                                                    throw new Exception();

                                                }

                                                long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                                long accountId = Account.getId(publicKey);

                                                BidOrder orderData = bidOrders.get(order);
                                                if (orderData == null || orderData.account.id != accountId) {

                                                    response.put("errorCode", 5);
                                                    response.put("errorDescription", "Unknown order");

                                                } else {

                                                    Account account = accounts.get(accountId);
                                                    if (account == null) {

                                                        response.put("errorCode", 6);
                                                        response.put("errorDescription", "Not enough funds");

                                                    } else {

                                                        if (fee * 100L > account.getUnconfirmedBalance()) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            int timestamp = getEpochTime(System.currentTimeMillis());

                                                            Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
                                                            transaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(order);
                                                            transaction.sign(secretPhrase);

                                                            JSONObject peerRequest = new JSONObject();
                                                            peerRequest.put("requestType", "processTransactions");
                                                            JSONArray transactionsData = new JSONArray();
                                                            transactionsData.add(transaction.getJSONObject());
                                                            peerRequest.put("transactions", transactionsData);

                                                            Peer.sendToSomePeers(peerRequest);

                                                            response.put("transaction", transaction.getStringId());

                                                            nonBroadcastedTransactions.put(transaction.id, transaction);

                                                        }

                                                    }

                                                }

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"deadline\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"fee\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"order\"");

                                    }

                                }

                            }
                            break;

                        case "getAsset":
                            {

                                String asset = req.getParameter("asset");
                                if (asset == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"asset\" not specified");

                                } else {

                                    try {

                                        Asset assetData = assets.get(Convert.parseUnsignedLong(asset));
                                        if (assetData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown asset");

                                        } else {

                                            response.put("account", Convert.convert(assetData.accountId));
                                            response.put("name", assetData.name);
                                            if (assetData.description.length() > 0) {

                                                response.put("description", assetData.description);

                                            }
                                            response.put("quantity", assetData.quantity);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"asset\"");

                                    }

                                }

                            }
                            break;

                        case "getAssetIds":
                            {

                                JSONArray assetIds = new JSONArray();
                                for (Long assetId : assets.keySet()) {

                                    assetIds.add(Convert.convert(assetId));

                                }
                                response.put("assetIds", assetIds);

                            }
                            break;

                        case "transferAsset":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String recipientValue = req.getParameter("recipient");
                                String assetValue = req.getParameter("asset");
                                String quantityValue = req.getParameter("quantity");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (recipientValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"recipient\" not specified");

                                } else if (assetValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"asset\" not specified");

                                } else if (quantityValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"quantity\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    try {

                                        long recipient = Convert.parseUnsignedLong(recipientValue);

                                        try {

                                            long asset = Convert.parseUnsignedLong(assetValue);

                                            try {

                                                int quantity = Integer.parseInt(quantityValue);
                                                if (quantity <= 0 || quantity >= MAX_ASSET_QUANTITY) {

                                                    throw new Exception();

                                                }

                                                try {

                                                    int fee = Integer.parseInt(feeValue);
                                                    if (fee <= 0 || fee >= MAX_BALANCE) {

                                                        throw new Exception();

                                                    }

                                                    try {

                                                        short deadline = Short.parseShort(deadlineValue);
                                                        if (deadline < 1) {

                                                            throw new Exception();

                                                        }

                                                        long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

                                                        Account account = accounts.get(Account.getId(publicKey));
                                                        if (account == null) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            if (fee * 100L > account.getUnconfirmedBalance()) {

                                                                response.put("errorCode", 6);
                                                                response.put("errorDescription", "Not enough funds");

                                                            } else {

                                                                Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
                                                                if (assetBalance == null || quantity > assetBalance) {

                                                                    response.put("errorCode", 6);
                                                                    response.put("errorDescription", "Not enough funds");

                                                                } else {

                                                                    int timestamp = getEpochTime(System.currentTimeMillis());

                                                                    Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER, timestamp, deadline, publicKey, recipient, 0, fee, referencedTransaction, new byte[64]);
                                                                    transaction.attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
                                                                    transaction.sign(secretPhrase);

                                                                    JSONObject peerRequest = new JSONObject();
                                                                    peerRequest.put("requestType", "processTransactions");
                                                                    JSONArray transactionsData = new JSONArray();
                                                                    transactionsData.add(transaction.getJSONObject());
                                                                    peerRequest.put("transactions", transactionsData);

                                                                    Peer.sendToSomePeers(peerRequest);

                                                                    response.put("transaction", transaction.getStringId());

                                                                    nonBroadcastedTransactions.put(transaction.id, transaction);

                                                                }

                                                            }

                                                        }

                                                    } catch (Exception e) {

                                                        response.put("errorCode", 4);
                                                        response.put("errorDescription", "Incorrect \"deadline\""+e.toString());

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"fee\"");

                                                }

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"quantity\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"asset\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"recipient\"");

                                    }

                                }

                            }
                            break;

                        case "placeAskOrder":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String assetValue = req.getParameter("asset");
                                String quantityValue = req.getParameter("quantity");
                                String priceValue = req.getParameter("price");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (assetValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"asset\" not specified");

                                } else if (quantityValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"quantity\" not specified");

                                } else if (priceValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"price\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    try {

                                        long price = Long.parseLong(priceValue);
                                        if (price <= 0 || price > MAX_BALANCE * 100L) {

                                            throw new Exception();

                                        }

                                        try {

                                            long asset = Convert.parseUnsignedLong(assetValue);

                                            try {

                                                int quantity = Integer.parseInt(quantityValue);
                                                if (quantity <= 0 || quantity >= MAX_ASSET_QUANTITY) {

                                                    throw new Exception();

                                                }

                                                try {

                                                    int fee = Integer.parseInt(feeValue);
                                                    if (fee <= 0 || fee >= MAX_BALANCE) {

                                                        throw new Exception();

                                                    }

                                                    try {

                                                        short deadline = Short.parseShort(deadlineValue);
                                                        if (deadline < 1) {

                                                            throw new Exception();

                                                        }

                                                        long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

                                                        Account account = accounts.get(Account.getId(publicKey));
                                                        if (account == null) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            if (fee * 100L > account.getUnconfirmedBalance()) {

                                                                response.put("errorCode", 6);
                                                                response.put("errorDescription", "Not enough funds");

                                                            } else {

                                                                Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
                                                                if (assetBalance == null || quantity > assetBalance) {

                                                                    response.put("errorCode", 6);
                                                                    response.put("errorDescription", "Not enough funds");

                                                                } else {

                                                                    int timestamp = getEpochTime(System.currentTimeMillis());

                                                                    Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
                                                                    transaction.attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, price);
                                                                    transaction.sign(secretPhrase);

                                                                    JSONObject peerRequest = new JSONObject();
                                                                    peerRequest.put("requestType", "processTransactions");
                                                                    JSONArray transactionsData = new JSONArray();
                                                                    transactionsData.add(transaction.getJSONObject());
                                                                    peerRequest.put("transactions", transactionsData);

                                                                    Peer.sendToSomePeers(peerRequest);

                                                                    response.put("transaction", transaction.getStringId());

                                                                    nonBroadcastedTransactions.put(transaction.id, transaction);

                                                                }

                                                            }

                                                        }

                                                    } catch (Exception e) {

                                                        response.put("errorCode", 4);
                                                        response.put("errorDescription", "Incorrect \"deadline\"");

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"fee\"");

                                                }

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"quantity\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"asset\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"price\"");

                                    }

                                }

                            }
                            break;

                        case "placeBidOrder":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String assetValue = req.getParameter("asset");
                                String quantityValue = req.getParameter("quantity");
                                String priceValue = req.getParameter("price");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (assetValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"asset\" not specified");

                                } else if (quantityValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"quantity\" not specified");

                                } else if (priceValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"price\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    try {

                                        long price = Long.parseLong(priceValue);
                                        if (price <= 0 || price > MAX_BALANCE * 100L) {

                                            throw new Exception();

                                        }

                                        try {

                                            long asset = Convert.parseUnsignedLong(assetValue);

                                            try {

                                                int quantity = Integer.parseInt(quantityValue);
                                                if (quantity <= 0 || quantity >= MAX_ASSET_QUANTITY) {

                                                    throw new Exception();

                                                }

                                                try {

                                                    int fee = Integer.parseInt(feeValue);
                                                    if (fee <= 0 || fee >= MAX_BALANCE) {

                                                        throw new Exception();

                                                    }

                                                    try {

                                                        short deadline = Short.parseShort(deadlineValue);
                                                        if (deadline < 1) {

                                                            throw new Exception();

                                                        }

                                                        long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

                                                        Account account = accounts.get(Account.getId(publicKey));
                                                        if (account == null) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            if (quantity * price + fee * 100L > account.getUnconfirmedBalance()) {

                                                                response.put("errorCode", 6);
                                                                response.put("errorDescription", "Not enough funds");

                                                            } else {

                                                                int timestamp = getEpochTime(System.currentTimeMillis());

                                                                Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT, timestamp, deadline, publicKey, CREATOR_ID, 0, fee, referencedTransaction, new byte[64]);
                                                                transaction.attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
                                                                transaction.sign(secretPhrase);

                                                                JSONObject peerRequest = new JSONObject();
                                                                peerRequest.put("requestType", "processTransactions");
                                                                JSONArray transactionsData = new JSONArray();
                                                                transactionsData.add(transaction.getJSONObject());
                                                                peerRequest.put("transactions", transactionsData);

                                                                Peer.sendToSomePeers(peerRequest);

                                                                response.put("transaction", transaction.getStringId());

                                                                nonBroadcastedTransactions.put(transaction.id, transaction);

                                                            }

                                                        }

                                                    } catch (Exception e) {

                                                        response.put("errorCode", 4);
                                                        response.put("errorDescription", "Incorrect \"deadline\"");

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"fee\"");

                                                }

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"quantity\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"asset\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"price\"");

                                    }

                                }

                            }
                            break;

                            // TODO: comment ends here

                            case "getAccountCurrentAskOrderIds":
                            {

                                String account = req.getParameter("account");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown account");

                                        } else {

                                            boolean assetIsNotUsed;
                                            long assetId;
                                            try {

                                                assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
                                                assetIsNotUsed = false;

                                            } catch (Exception e) {

                                                assetId = 0;
                                                assetIsNotUsed = true;

                                            }

                                            JSONArray orderIds = new JSONArray();
                                            for (AskOrder askOrder : askOrders.values()) {

                                                if ((assetIsNotUsed || askOrder.asset == assetId) && askOrder.account == accountData) {

                                                    orderIds.add(Convert.convert(askOrder.id));

                                                }

                                            }
                                            response.put("askOrderIds", orderIds);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getAccountCurrentBidOrderIds":
                            {

                                String account = req.getParameter("account");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else {

                                    try {

                                        Account accountData = accounts.get(Convert.parseUnsignedLong(account));
                                        if (accountData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown account");

                                        } else {

                                            boolean assetIsNotUsed;
                                            long assetId;
                                            try {

                                                assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
                                                assetIsNotUsed = false;

                                            } catch (Exception e) {

                                                assetId = 0;
                                                assetIsNotUsed = true;

                                            }

                                            JSONArray orderIds = new JSONArray();
                                            for (BidOrder bidOrder : bidOrders.values()) {

                                                if ((assetIsNotUsed || bidOrder.asset == assetId) && bidOrder.account == accountData) {

                                                    orderIds.add(Convert.convert(bidOrder.id));

                                                }

                                            }
                                            response.put("bidOrderIds", orderIds);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "getAskOrder":
                            {

                                String order = req.getParameter("order");
                                if (order == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"order\" not specified");

                                } else {

                                    try {

                                        AskOrder orderData = askOrders.get(Convert.parseUnsignedLong(order));
                                        if (orderData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown ask order");

                                        } else {

                                            response.put("account", Convert.convert(orderData.account.id));
                                            response.put("asset", Convert.convert(orderData.asset));
                                            response.put("quantity", orderData.quantity);
                                            response.put("price", orderData.price);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"order\"");

                                    }

                                }

                            }
                            break;

                        case "getAskOrderIds":
                            {

                                JSONArray orderIds = new JSONArray();
                                for (Long orderId : askOrders.keySet()) {

                                    orderIds.add(Convert.convert(orderId));

                                }
                                response.put("askOrderIds", orderIds);

                            }
                            break;

                        case "getBidOrder":
                            {

                                String order = req.getParameter("order");
                                if (order == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"order\" not specified");

                                } else {

                                    try {

                                        BidOrder orderData = bidOrders.get(Convert.parseUnsignedLong(order));
                                        if (orderData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown bid order");

                                        } else {

                                            response.put("account", Convert.convert(orderData.account.id));
                                            response.put("asset", Convert.convert(orderData.asset));
                                            response.put("quantity", orderData.quantity);
                                            response.put("price", orderData.price);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"order\"");

                                    }

                                }

                            }
                            break;

                        case "getBidOrderIds":
                            {

                                JSONArray orderIds = new JSONArray();
                                for (Long orderId : bidOrders.keySet()) {

                                    orderIds.add(Convert.convert(orderId));

                                }
                                response.put("bidOrderIds", orderIds);

                            }
                            break;

                            case "listAccountAliases":
                            {

                                String account = req.getParameter("account");
                                if (account == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"account\" not specified");

                                } else {

                                    try {

                                        long accountId = Convert.parseUnsignedLong(account);
                                        Account accountData = accounts.get(accountId);
                                        if (accountData == null) {

                                            response.put("errorCode", 5);
                                            response.put("errorDescription", "Unknown account");

                                        } else {

                                            JSONArray aliases = new JSONArray();
                                            for (Alias alias : Nxt.aliases.values()) {

                                                if (alias.account.id == accountId) {

                                                    JSONObject aliasData = new JSONObject();
                                                    aliasData.put("alias", alias.alias);
                                                    aliasData.put("uri", alias.uri);
                                                    aliases.add(aliasData);

                                                }

                                            }
                                            response.put("aliases", aliases);

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"account\"");

                                    }

                                }

                            }
                            break;

                            case "markHost":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String host = req.getParameter("host");
                                String weightValue = req.getParameter("weight");
                                String dateValue = req.getParameter("date");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (host == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"host\" not specified");

                                } else if (weightValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"weight\" not specified");

                                } else if (dateValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"date\" not specified");

                                } else {

                                    if (host.length() > 100) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"host\" (the length exceeds 100 chars limit)");

                                    } else {

                                        try {

                                            int weight = Integer.parseInt(weightValue);
                                            if (weight <= 0 || weight > MAX_BALANCE) {

                                                throw new Exception();

                                            }

                                            try {

                                                int date = Integer.parseInt(dateValue.substring(0, 4)) * 10000 + Integer.parseInt(dateValue.substring(5, 7)) * 100 + Integer.parseInt(dateValue.substring(8, 10));

                                                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                                                byte[] hostBytes = host.getBytes("UTF-8");

                                                ByteBuffer buffer = ByteBuffer.allocate(32 + 2 + hostBytes.length + 4 + 4 + 1);
                                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                buffer.put(publicKey);
                                                buffer.putShort((short)hostBytes.length);
                                                buffer.put(hostBytes);
                                                buffer.putInt(weight);
                                                buffer.putInt(date);

                                                byte[] data = buffer.array();
                                                byte[] signature;
                                                do {
                                                    data[data.length - 1] = (byte)ThreadLocalRandom.current().nextInt();
                                                    signature = Crypto.sign(data, secretPhrase);
                                                } while (!Crypto.verify(signature, data, publicKey));

                                                response.put("hallmark", Convert.convert(data) + Convert.convert(signature));

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"date\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"weight\"");

                                        }

                                    }

                                }

                            }
                            break;

                            case "sendMessage":
                            {

                                String secretPhrase = req.getParameter("secretPhrase");
                                String recipientValue = req.getParameter("recipient");
                                String messageValue = req.getParameter("message");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (recipientValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"recipient\" not specified");

                                } else if (messageValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"message\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    try {

                                        long recipient = Convert.parseUnsignedLong(recipientValue);

                                        try {

                                            byte[] message = Convert.convert(messageValue);
                                            if (message.length > MAX_ARBITRARY_MESSAGE_LENGTH) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"message\" (length must be not longer than " + MAX_ARBITRARY_MESSAGE_LENGTH + " bytes)");

                                            } else {

                                                try {

                                                    int fee = Integer.parseInt(feeValue);
                                                    if (fee <= 0 || fee >= MAX_BALANCE) {

                                                        throw new Exception();

                                                    }

                                                    try {

                                                        short deadline = Short.parseShort(deadlineValue);
                                                        if (deadline < 1) {

                                                            throw new Exception();

                                                        }

                                                        long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                        byte[] publicKey = Crypto.getPublicKey(secretPhrase);

                                                        Account account = accounts.get(Account.getId(publicKey));
                                                        if (account == null || fee * 100L > account.getUnconfirmedBalance()) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            int timestamp = getEpochTime(System.currentTimeMillis());

                                                            Transaction transaction = new Transaction(Transaction.TYPE_MESSAGING, Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE, timestamp, deadline, publicKey, recipient, 0, fee, referencedTransaction, new byte[64]);
                                                            transaction.attachment = new Attachment.MessagingArbitraryMessage(message);
                                                            transaction.sign(secretPhrase);

                                                            JSONObject peerRequest = new JSONObject();
                                                            peerRequest.put("requestType", "processTransactions");
                                                            JSONArray transactionsData = new JSONArray();
                                                            transactionsData.add(transaction.getJSONObject());
                                                            peerRequest.put("transactions", transactionsData);

                                                            Peer.sendToSomePeers(peerRequest);

                                                            response.put("transaction", transaction.getStringId());
                                                            response.put("bytes", Convert.convert(transaction.getBytes()));

                                                            nonBroadcastedTransactions.put(transaction.id, transaction);

                                                        }

                                                    } catch (Exception e) {

                                                        response.put("errorCode", 4);
                                                        response.put("errorDescription", "Incorrect \"deadline\"");

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"fee\"");

                                                }

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"message\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"recipient\"");

                                    }

                                }

                            }
                            break;

                            case "sendMoney":
                            {

                                /*if (bootstrappingState < 0) {

                                    response.put("errorCode", 2);
                                    response.put("errorDescription", "Blockchain not up to date");

                                    break;

                                }*/

                                String secretPhrase = req.getParameter("secretPhrase");
                                String recipientValue = req.getParameter("recipient");
                                String amountValue = req.getParameter("amount");
                                String feeValue = req.getParameter("fee");
                                String deadlineValue = req.getParameter("deadline");
                                String referencedTransactionValue = req.getParameter("referencedTransaction");
                                if (secretPhrase == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"secretPhrase\" not specified");

                                } else if (recipientValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"recipient\" not specified");

                                } else if (amountValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"amount\" not specified");

                                } else if (feeValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"fee\" not specified");

                                } else if (deadlineValue == null) {

                                    response.put("errorCode", 3);
                                    response.put("errorDescription", "\"deadline\" not specified");

                                } else {

                                    //TODO: fix ugly error handling
                                    try {

                                        long recipient = Convert.parseUnsignedLong(recipientValue);

                                        try {

                                            int amount = Integer.parseInt(amountValue);
                                            if (amount <= 0 || amount >= MAX_BALANCE) {

                                                throw new Exception();

                                            }

                                            try {

                                                int fee = Integer.parseInt(feeValue);
                                                if (fee <= 0 || fee >= MAX_BALANCE) {

                                                    throw new Exception();

                                                }

                                                try {

                                                    short deadline = Short.parseShort(deadlineValue);
                                                    if (deadline < 1) {

                                                        throw new Exception();

                                                    }

                                                    long referencedTransaction = referencedTransactionValue == null ? 0 : Convert.parseUnsignedLong(referencedTransactionValue);

                                                    byte[] publicKey = Crypto.getPublicKey(secretPhrase);

                                                    Account account = accounts.get(Account.getId(publicKey));
                                                    if (account == null) {

                                                        response.put("errorCode", 6);
                                                        response.put("errorDescription", "Not enough funds");

                                                    } else {

                                                        if ((amount + fee) * 100L > account.getUnconfirmedBalance()) {

                                                            response.put("errorCode", 6);
                                                            response.put("errorDescription", "Not enough funds");

                                                        } else {

                                                            Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, getEpochTime(System.currentTimeMillis()), deadline, publicKey, recipient, amount, fee, referencedTransaction, new byte[64]);
                                                            transaction.sign(secretPhrase);

                                                            JSONObject peerRequest = new JSONObject();
                                                            peerRequest.put("requestType", "processTransactions");
                                                            JSONArray transactionsData = new JSONArray();
                                                            transactionsData.add(transaction.getJSONObject());
                                                            peerRequest.put("transactions", transactionsData);

                                                            Peer.sendToSomePeers(peerRequest);

                                                            response.put("transaction", transaction.getStringId());
                                                            response.put("bytes", Convert.convert(transaction.getBytes()));

                                                            nonBroadcastedTransactions.put(transaction.id, transaction);

                                                        }

                                                    }

                                                } catch (Exception e) {

                                                    response.put("errorCode", 4);
                                                    response.put("errorDescription", "Incorrect \"deadline\"");

                                                }

                                            } catch (Exception e) {

                                                response.put("errorCode", 4);
                                                response.put("errorDescription", "Incorrect \"fee\"");

                                            }

                                        } catch (Exception e) {

                                            response.put("errorCode", 4);
                                            response.put("errorDescription", "Incorrect \"amount\"");

                                        }

                                    } catch (Exception e) {

                                        response.put("errorCode", 4);
                                        response.put("errorDescription", "Incorrect \"recipient\"");

                                    }

                                }

                            }
                            break;

                            default:
                            {

                                response.put("errorCode", 1);
                                response.put("errorDescription", "Incorrect request");

                            }

                        }

                    }

                }

                resp.setContentType("text/plain; charset=UTF-8");

                try (Writer writer = resp.getWriter()) {
                    response.writeJSONString(writer);
                }

                return;

            } else { // userPasscode != null

                if (allowedUserHosts != null && !allowedUserHosts.contains(req.getRemoteHost())) {

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

                user = users.get(userPasscode);
                if (user == null) {

                    user = new User();
                    User oldUser = users.putIfAbsent(userPasscode, user);
                    if (oldUser != null) {
                        user = oldUser;
                        user.isInactive = false;
                    }

                } else {

                    user.isInactive = false; // make sure to activate dormant user

                }

            }

            switch (req.getParameter("requestType")) {

                case "generateAuthorizationToken":
                {
                    String secretPhrase = req.getParameter("secretPhrase");
                    if (! user.secretPhrase.equals(secretPhrase)) {
                        JSONObject response = new JSONObject();
                        response.put("response", "showMessage");
                        response.put("message", "Invalid secret phrase!");
                        user.pendingResponses.offer(response);
                        break;
                    }
                    byte[] website = req.getParameter("website").trim().getBytes("UTF-8");
                    byte[] data = new byte[website.length + 32 + 4];
                    System.arraycopy(website, 0, data, 0, website.length);
                    System.arraycopy(user.publicKey, 0, data, website.length, 32);
                    int timestamp = getEpochTime(System.currentTimeMillis());
                    data[website.length + 32] = (byte)timestamp;
                    data[website.length + 32 + 1] = (byte)(timestamp >> 8);
                    data[website.length + 32 + 2] = (byte)(timestamp >> 16);
                    data[website.length + 32 + 3] = (byte)(timestamp >> 24);

                    byte[] token = new byte[100];
                    System.arraycopy(data, website.length, token, 0, 32 + 4);
                    System.arraycopy(Crypto.sign(data, user.secretPhrase), 0, token, 32 + 4, 64);
                    String tokenString = "";
                    for (int ptr = 0; ptr < 100; ptr += 5) {

                        long number = ((long)(token[ptr] & 0xFF)) | (((long)(token[ptr + 1] & 0xFF)) << 8) | (((long)(token[ptr + 2] & 0xFF)) << 16) | (((long)(token[ptr + 3] & 0xFF)) << 24) | (((long)(token[ptr + 4] & 0xFF)) << 32);
                        if (number < 32) {

                            tokenString += "0000000";

                        } else if (number < 1024) {

                            tokenString += "000000";

                        } else if (number < 32768) {

                            tokenString += "00000";

                        } else if (number < 1048576) {

                            tokenString += "0000";

                        } else if (number < 33554432) {

                            tokenString += "000";

                        } else if (number < 1073741824) {

                            tokenString += "00";

                        } else if (number < 34359738368L) {

                            tokenString += "0";

                        }
                        tokenString += Long.toString(number, 32);

                    }

                    JSONObject response = new JSONObject();
                    response.put("response", "showAuthorizationToken");
                    response.put("token", tokenString);

                    user.pendingResponses.offer(response);

                }
                break;

                case "getInitialData":
                {

                    JSONArray unconfirmedTransactions = new JSONArray();
                    JSONArray activePeers = new JSONArray(), knownPeers = new JSONArray(), blacklistedPeers = new JSONArray();
                    JSONArray recentBlocks = new JSONArray();

                    for (Transaction transaction : Nxt.unconfirmedTransactions.values()) {

                        JSONObject unconfirmedTransaction = new JSONObject();
                        unconfirmedTransaction.put("index", transaction.index);
                        unconfirmedTransaction.put("timestamp", transaction.timestamp);
                        unconfirmedTransaction.put("deadline", transaction.deadline);
                        unconfirmedTransaction.put("recipient", Convert.convert(transaction.recipient));
                        unconfirmedTransaction.put("amount", transaction.amount);
                        unconfirmedTransaction.put("fee", transaction.fee);
                        unconfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));

                        unconfirmedTransactions.add(unconfirmedTransaction);

                    }

                    for (Map.Entry<String, Peer> peerEntry : peers.entrySet()) {

                        String address = peerEntry.getKey();
                        Peer peer = peerEntry.getValue();

                        if (peer.blacklistingTime > 0) {

                            JSONObject blacklistedPeer = new JSONObject();
                            blacklistedPeer.put("index", peer.index);
                            blacklistedPeer.put("announcedAddress", peer.announcedAddress.length() > 0 ? (peer.announcedAddress.length() > 30 ? (peer.announcedAddress.substring(0, 30) + "...") : peer.announcedAddress) : address);
                            for (String wellKnownPeer : wellKnownPeers) {

                                if (peer.announcedAddress.equals(wellKnownPeer)) {

                                    blacklistedPeer.put("wellKnown", true);

                                    break;

                                }

                            }

                            blacklistedPeers.add(blacklistedPeer);

                        } else if (peer.state == Peer.STATE_NONCONNECTED) {

                            if (peer.announcedAddress.length() > 0) {

                                JSONObject knownPeer = new JSONObject();
                                knownPeer.put("index", peer.index);
                                knownPeer.put("announcedAddress", peer.announcedAddress.length() > 30 ? (peer.announcedAddress.substring(0, 30) + "...") : peer.announcedAddress);
                                for (String wellKnownPeer : wellKnownPeers) {

                                    if (peer.announcedAddress.equals(wellKnownPeer)) {

                                        knownPeer.put("wellKnown", true);

                                        break;

                                    }

                                }

                                knownPeers.add(knownPeer);

                            }

                        } else {

                            JSONObject activePeer = new JSONObject();
                            activePeer.put("index", peer.index);
                            if (peer.state == peer.STATE_DISCONNECTED) {

                                activePeer.put("disconnected", true);

                            }
                            activePeer.put("address", address.length() > 30 ? (address.substring(0, 30) + "...") : address);
                            activePeer.put("announcedAddress", peer.announcedAddress.length() > 30 ? (peer.announcedAddress.substring(0, 30) + "...") : peer.announcedAddress);
                            activePeer.put("weight", peer.getWeight());
                            activePeer.put("downloaded", peer.downloadedVolume);
                            activePeer.put("uploaded", peer.uploadedVolume);
                            activePeer.put("software", peer.getSoftware());
                            for (String wellKnownPeer : wellKnownPeers) {

                                if (peer.announcedAddress.equals(wellKnownPeer)) {

                                    activePeer.put("wellKnown", true);

                                    break;

                                }

                            }

                            activePeers.add(activePeer);

                        }

                    }

                    long blockId = lastBlock.get().getId();
                    int numberOfBlocks = 0;
                    while (numberOfBlocks < 60) {

                        numberOfBlocks++;

                        Block block = blocks.get(blockId);
                        JSONObject recentBlock = new JSONObject();
                        recentBlock.put("index", block.index);
                        recentBlock.put("timestamp", block.timestamp);
                        recentBlock.put("numberOfTransactions", block.transactions.length);
                        recentBlock.put("totalAmount", block.totalAmount);
                        recentBlock.put("totalFee", block.totalFee);
                        recentBlock.put("payloadLength", block.payloadLength);
                        recentBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
                        recentBlock.put("height", block.height);
                        recentBlock.put("version", block.version);
                        recentBlock.put("block", block.getStringId());
                        recentBlock.put("baseTarget", BigInteger.valueOf(block.baseTarget).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(initialBaseTarget)));

                        recentBlocks.add(recentBlock);

                        if (blockId == GENESIS_BLOCK_ID) {

                            break;

                        }

                        blockId = block.previousBlock;

                    }

                    JSONObject response = new JSONObject();
                    response.put("response", "processInitialData");
                    response.put("version", VERSION);
                    if (unconfirmedTransactions.size() > 0) {

                        response.put("unconfirmedTransactions", unconfirmedTransactions);

                    }
                    if (activePeers.size() > 0) {

                        response.put("activePeers", activePeers);

                    }
                    if (knownPeers.size() > 0) {

                        response.put("knownPeers", knownPeers);

                    }
                    if (blacklistedPeers.size() > 0) {

                        response.put("blacklistedPeers", blacklistedPeers);

                    }
                    if (recentBlocks.size() > 0) {

                        response.put("recentBlocks", recentBlocks);

                    }

                    user.pendingResponses.offer(response);

                }
                break;

                case "getNewData":
                    break;

                case "lockAccount":
                {

                    user.deinitializeKeyPair();

                    JSONObject response = new JSONObject();
                    response.put("response", "lockAccount");

                    user.pendingResponses.offer(response);

                }
                break;

                case "removeActivePeer":
                {

                    if (allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {

                        JSONObject response = new JSONObject();
                        response.put("response", "showMessage");
                        response.put("message", "This operation is allowed to local host users only!");

                        user.pendingResponses.offer(response);

                    } else {

                        int index = Integer.parseInt(req.getParameter("peer"));
                        for (Peer peer : peers.values()) {

                            if (peer.index == index) {

                                if (peer.blacklistingTime == 0 && peer.state != Peer.STATE_NONCONNECTED) {

                                    peer.deactivate();

                                }

                                break;

                            }

                        }

                    }

                }
                break;

                case "removeBlacklistedPeer":
                {

                    if (allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {

                        JSONObject response = new JSONObject();
                        response.put("response", "showMessage");
                        response.put("message", "This operation is allowed to local host users only!");

                        user.pendingResponses.offer(response);

                    } else {

                        int index = Integer.parseInt(req.getParameter("peer"));
                        for (Peer peer : peers.values()) {

                            if (peer.index == index) {

                                if (peer.blacklistingTime > 0) {

                                    peer.removeBlacklistedStatus();

                                }

                                break;

                            }

                        }

                    }

                }
                break;

                case "removeKnownPeer":
                {

                    if (allowedUserHosts == null && !InetAddress.getByName(req.getRemoteAddr()).isLoopbackAddress()) {

                        JSONObject response = new JSONObject();
                        response.put("response", "showMessage");
                        response.put("message", "This operation is allowed to local host users only!");

                        user.pendingResponses.offer(response);

                    } else {

                        int index = Integer.parseInt(req.getParameter("peer"));
                        for (Peer peer : peers.values()) {

                            if (peer.index == index) {

                                peer.removePeer();

                                break;

                            }

                        }

                    }

                }
                break;

                case "sendMoney":
                {

                    if (user.secretPhrase != null) {

                        String recipientValue = req.getParameter("recipient"), amountValue = req.getParameter("amount"), feeValue = req.getParameter("fee"), deadlineValue = req.getParameter("deadline");
                        String secretPhrase = req.getParameter("secretPhrase");

                        long recipient;
                        int amount = 0, fee = 0;
                        short deadline = 0;

                        try {

                            recipient = Convert.parseUnsignedLong(recipientValue);
                            amount = Integer.parseInt(amountValue.trim());
                            fee = Integer.parseInt(feeValue.trim());
                            deadline = (short)(Double.parseDouble(deadlineValue) * 60);

                        } catch (Exception e) {

                            JSONObject response = new JSONObject();
                            response.put("response", "notifyOfIncorrectTransaction");
                            response.put("message", "One of the fields is filled incorrectly!");
                            response.put("recipient", recipientValue);
                            response.put("amount", amountValue);
                            response.put("fee", feeValue);
                            response.put("deadline", deadlineValue);

                            user.pendingResponses.offer(response);

                            break;

                        }

                        if (! user.secretPhrase.equals(secretPhrase)) {

                            JSONObject response = new JSONObject();
                            response.put("response", "notifyOfIncorrectTransaction");
                            response.put("message", "Wrong secret phrase!");
                            response.put("recipient", recipientValue);
                            response.put("amount", amountValue);
                            response.put("fee", feeValue);
                            response.put("deadline", deadlineValue);

                            user.pendingResponses.offer(response);

                        } else if (amount <= 0 || amount > MAX_BALANCE) {

                            JSONObject response = new JSONObject();
                            response.put("response", "notifyOfIncorrectTransaction");
                            response.put("message", "\"Amount\" must be greater than 0!");
                            response.put("recipient", recipientValue);
                            response.put("amount", amountValue);
                            response.put("fee", feeValue);
                            response.put("deadline", deadlineValue);

                            user.pendingResponses.offer(response);

                        } else if (fee <= 0 || fee > MAX_BALANCE) {

                            JSONObject response = new JSONObject();
                            response.put("response", "notifyOfIncorrectTransaction");
                            response.put("message", "\"Fee\" must be greater than 0!");
                            response.put("recipient", recipientValue);
                            response.put("amount", amountValue);
                            response.put("fee", feeValue);
                            response.put("deadline", deadlineValue);

                            user.pendingResponses.offer(response);

                        } else if (deadline < 1) {

                            JSONObject response = new JSONObject();
                            response.put("response", "notifyOfIncorrectTransaction");
                            response.put("message", "\"Deadline\" must be greater or equal to 1 minute!");
                            response.put("recipient", recipientValue);
                            response.put("amount", amountValue);
                            response.put("fee", feeValue);
                            response.put("deadline", deadlineValue);

                            user.pendingResponses.offer(response);

                        } else {

                            Account account = accounts.get(Account.getId(user.publicKey));
                            if (account == null || (amount + fee) * 100L > account.getUnconfirmedBalance()) {

                                JSONObject response = new JSONObject();
                                response.put("response", "notifyOfIncorrectTransaction");
                                response.put("message", "Not enough funds!");
                                response.put("recipient", recipientValue);
                                response.put("amount", amountValue);
                                response.put("fee", feeValue);
                                response.put("deadline", deadlineValue);

                                user.pendingResponses.offer(response);

                            } else {

                                final Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, getEpochTime(System.currentTimeMillis()), deadline, user.publicKey, recipient, amount, fee, 0, new byte[64]);
                                transaction.sign(user.secretPhrase);

                                JSONObject peerRequest = new JSONObject();
                                peerRequest.put("requestType", "processTransactions");
                                JSONArray transactionsData = new JSONArray();
                                transactionsData.add(transaction.getJSONObject());
                                peerRequest.put("transactions", transactionsData);

                                Peer.sendToSomePeers(peerRequest);

                                JSONObject response = new JSONObject();
                                response.put("response", "notifyOfAcceptedTransaction");

                                user.pendingResponses.offer(response);

                                nonBroadcastedTransactions.put(transaction.id, transaction);

                            }

                        }

                    }

                }
                break;

                case "unlockAccount":
                {

                    String secretPhrase = req.getParameter("secretPhrase");
                    // lock all other instances of this account being unlocked
                    for (User u : Nxt.users.values()) {
                        if (secretPhrase.equals(u.secretPhrase)) {
                            u.deinitializeKeyPair();
                            if (! u.isInactive) {
                                JSONObject response = new JSONObject();
                                response.put("response", "lockAccount");
                                u.pendingResponses.offer(response);
                            }
                        }
                    }

                    BigInteger bigInt = user.initializeKeyPair(secretPhrase);
                    long accountId = bigInt.longValue();

                    JSONObject response = new JSONObject();
                    response.put("response", "unlockAccount");
                    response.put("account", bigInt.toString());

                    if (secretPhrase.length() < 30) {

                        response.put("secretPhraseStrength", 1);

                    } else {

                        response.put("secretPhraseStrength", 5);

                    }

                    Account account = accounts.get(accountId);
                    if (account == null) {

                        response.put("balance", 0);

                    } else {

                        response.put("balance", account.getUnconfirmedBalance());

                        long effectiveBalance = account.getEffectiveBalance();
                        if (effectiveBalance > 0) {

                            JSONObject response2 = new JSONObject();
                            response2.put("response", "setBlockGenerationDeadline");

                            Block lastBlock = Nxt.lastBlock.get();
                            MessageDigest digest = Crypto.getMessageDigest("SHA-256");
                            byte[] generationSignatureHash;
                            if (lastBlock.height < TRANSPARENT_FORGING_BLOCK) {

                                byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
                                generationSignatureHash = digest.digest(generationSignature);

                            } else {

                                digest.update(lastBlock.generationSignature);
                                generationSignatureHash = digest.digest(user.publicKey);

                            }
                            BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
                            response2.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (getEpochTime(System.currentTimeMillis()) - lastBlock.timestamp));

                            user.pendingResponses.offer(response2);

                        }

                        JSONArray myTransactions = new JSONArray();
                        byte[] accountPublicKey = account.publicKey.get();
                        for (Transaction transaction : unconfirmedTransactions.values()) {

                            if (Arrays.equals(transaction.senderPublicKey, accountPublicKey)) {

                                JSONObject myTransaction = new JSONObject();
                                myTransaction.put("index", transaction.index);
                                myTransaction.put("transactionTimestamp", transaction.timestamp);
                                myTransaction.put("deadline", transaction.deadline);
                                myTransaction.put("account", Convert.convert(transaction.recipient));
                                myTransaction.put("sentAmount", transaction.amount);
                                if (transaction.recipient == accountId) {

                                    myTransaction.put("receivedAmount", transaction.amount);

                                }
                                myTransaction.put("fee", transaction.fee);
                                myTransaction.put("numberOfConfirmations", 0);
                                myTransaction.put("id", transaction.getStringId());

                                myTransactions.add(myTransaction);

                            } else if (transaction.recipient == accountId) {

                                JSONObject myTransaction = new JSONObject();
                                myTransaction.put("index", transaction.index);
                                myTransaction.put("transactionTimestamp", transaction.timestamp);
                                myTransaction.put("deadline", transaction.deadline);
                                myTransaction.put("account", Convert.convert(transaction.getSenderAccountId()));
                                myTransaction.put("receivedAmount", transaction.amount);
                                myTransaction.put("fee", transaction.fee);
                                myTransaction.put("numberOfConfirmations", 0);
                                myTransaction.put("id", transaction.getStringId());

                                myTransactions.add(myTransaction);

                            }

                        }

                        long blockId = lastBlock.get().getId();
                        int numberOfConfirmations = 1;
                        while (myTransactions.size() < 1000) {

                            Block block = blocks.get(blockId);

                            if (block.totalFee > 0 && Arrays.equals(block.generatorPublicKey, accountPublicKey)) {

                                JSONObject myTransaction = new JSONObject();
                                myTransaction.put("index", block.getStringId()); // cfb: Generated fee transactions get an id equal to the block id
                                myTransaction.put("blockTimestamp", block.timestamp);
                                myTransaction.put("block", block.getStringId());
                                myTransaction.put("earnedAmount", block.totalFee);
                                myTransaction.put("numberOfConfirmations", numberOfConfirmations);
                                myTransaction.put("id", "-");

                                myTransactions.add(myTransaction);

                            }

                            for (Transaction transaction : block.blockTransactions) {

                                if (Arrays.equals(transaction.senderPublicKey, accountPublicKey)) {

                                    JSONObject myTransaction = new JSONObject();
                                    myTransaction.put("index", transaction.index);
                                    myTransaction.put("blockTimestamp", block.timestamp);
                                    myTransaction.put("transactionTimestamp", transaction.timestamp);
                                    myTransaction.put("account", Convert.convert(transaction.recipient));
                                    myTransaction.put("sentAmount", transaction.amount);
                                    if (transaction.recipient == accountId) {

                                        myTransaction.put("receivedAmount", transaction.amount);

                                    }
                                    myTransaction.put("fee", transaction.fee);
                                    myTransaction.put("numberOfConfirmations", numberOfConfirmations);
                                    myTransaction.put("id", transaction.getStringId());

                                    myTransactions.add(myTransaction);

                                } else if (transaction.recipient == accountId) {

                                    JSONObject myTransaction = new JSONObject();
                                    myTransaction.put("index", transaction.index);
                                    myTransaction.put("blockTimestamp", block.timestamp);
                                    myTransaction.put("transactionTimestamp", transaction.timestamp);
                                    myTransaction.put("account", Convert.convert(transaction.getSenderAccountId()));
                                    myTransaction.put("receivedAmount", transaction.amount);
                                    myTransaction.put("fee", transaction.fee);
                                    myTransaction.put("numberOfConfirmations", numberOfConfirmations);
                                    myTransaction.put("id", transaction.getStringId());

                                    myTransactions.add(myTransaction);

                                }

                            }

                            if (blockId == GENESIS_BLOCK_ID) {

                                break;

                            }

                            blockId = block.previousBlock;
                            numberOfConfirmations++;

                        }

                        if (myTransactions.size() > 0) {

                            JSONObject response2 = new JSONObject();
                            response2.put("response", "processNewData");
                            response2.put("addedMyTransactions", myTransactions);

                            user.pendingResponses.offer(response2);

                        }

                    }

                    user.pendingResponses.offer(response);

                }
                break;

                default:
                {

                    JSONObject response = new JSONObject();
                    response.put("response", "showMessage");
                    response.put("message", "Incorrect request!");

                    user.pendingResponses.offer(response);

                }

            }

        } catch (Exception e) {

            if (user != null) {

                Logger.logMessage("Error processing GET request", e);

                JSONObject response = new JSONObject();
                response.put("response", "showMessage");
                response.put("message", e.toString());

                user.pendingResponses.offer(response);

            } else {

                Logger.logDebugMessage("Error processing GET request", e);

            }

        }

        if (user != null) {

            synchronized (user) {

                JSONArray responses = new JSONArray();
                JSONObject pendingResponse;
                while ((pendingResponse = user.pendingResponses.poll()) != null) {

                    responses.add(pendingResponse);

                }

                if (responses.size() > 0) {

                    JSONObject combinedResponse = new JSONObject();
                    combinedResponse.put("responses", responses);

                    if (user.asyncContext != null) {

                        user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                        try (Writer writer = user.asyncContext.getResponse().getWriter()) {
                            combinedResponse.writeJSONString(writer);
                        }
                        user.asyncContext.complete();
                        user.asyncContext = req.startAsync();
                        user.asyncContext.addListener(user.new UserAsyncListener());
                        user.asyncContext.setTimeout(5000);

                    } else {

                        resp.setContentType("text/plain; charset=UTF-8");

                        try (Writer writer = resp.getWriter()) {
                            combinedResponse.writeJSONString(writer);
                        }

                    }

                } else {

                    if (user.asyncContext != null) {

                        user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                        try (Writer writer = user.asyncContext.getResponse().getWriter()) {
                            new JSONObject().writeJSONString(writer);
                        }

                        user.asyncContext.complete();

                    }

                    user.asyncContext = req.startAsync();
                    user.asyncContext.addListener(user.new UserAsyncListener());
                    user.asyncContext.setTimeout(5000);

                }

            }

        }

    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Peer peer = null;

        JSONObject response = new JSONObject();

        try {

            JSONObject request;
            {

                CountingInputStream cis = new CountingInputStream(req.getInputStream());

                try (Reader reader = new BufferedReader(new InputStreamReader(cis, "UTF-8"))) {
                    request = (JSONObject)JSONValue.parse(reader);
                }

                if (request == null) {
                    return;
                }

                peer = Peer.addPeer(req.getRemoteHost(), "");
                if (peer != null) {

                    if (peer.state == Peer.STATE_DISCONNECTED) {

                        peer.setState(Peer.STATE_CONNECTED);

                    }
                    peer.updateDownloadedVolume(cis.getCount());

                }

            }

            if (request.get("protocol") != null && ((Number)request.get("protocol")).intValue() == 1) { // autoboxing sucks

                switch ((String)request.get("requestType")) {

                    case "getCumulativeDifficulty":
                    {

                        response.put("cumulativeDifficulty", lastBlock.get().cumulativeDifficulty.toString());

                    }
                    break;

                    case "getInfo":
                    {

                        if (peer != null) {
                            String announcedAddress = (String)request.get("announcedAddress");
                            if (announcedAddress != null) {
                                announcedAddress = announcedAddress.trim();
                                if (announcedAddress.length() > 0) {

                                    peer.announcedAddress = announcedAddress;

                                }
                            }
                            String application = (String)request.get("application");
                            if (application == null) {

                                application = "?";

                            } else {

                                application = application.trim();
                                if (application.length() > 20) {

                                    application = application.substring(0, 20) + "...";

                                }

                            }
                            peer.application = application;

                            String version = (String)request.get("version");
                            if (version == null) {

                                version = "?";

                            } else {

                                version = version.trim();
                                if (version.length() > 10) {

                                    version = version.substring(0, 10) + "...";

                                }

                            }
                            peer.version = version;

                            String platform = (String)request.get("platform");
                            if (platform == null) {

                                platform = "?";

                            } else {

                                platform = platform.trim();
                                if (platform.length() > 10) {

                                    platform = platform.substring(0, 10) + "...";

                                }

                            }
                            peer.platform = platform;

                            peer.shareAddress = Boolean.TRUE.equals(request.get("shareAddress"));

                            if (peer.analyzeHallmark(req.getRemoteHost(), (String)request.get("hallmark"))) {

                                peer.setState(Peer.STATE_CONNECTED);

                            }

                        }

                        if (myHallmark != null && myHallmark.length() > 0) {

                            response.put("hallmark", myHallmark);

                        }
                        response.put("application", "NRS");
                        response.put("version", VERSION);
                        response.put("platform", myPlatform);
                        response.put("shareAddress", shareMyAddress);

                    }
                    break;

                    case "getMilestoneBlockIds":
                    {

                        JSONArray milestoneBlockIds = new JSONArray();
                        Block block = lastBlock.get();
                        int jumpLength = block.height * 4 / 1461 + 1;
                        while (block.height > 0) {

                            milestoneBlockIds.add(block.getStringId());
                            for (int i = 0; i < jumpLength && block.height > 0; i++) {

                                block = blocks.get(block.previousBlock);

                            }

                        }
                        response.put("milestoneBlockIds", milestoneBlockIds);

                    }
                    break;

                    case "getNextBlockIds":
                    {

                        JSONArray nextBlockIds = new JSONArray();
                        Block block = blocks.get(Convert.parseUnsignedLong((String) request.get("blockId")));
                        while (block != null && nextBlockIds.size() < 1440) {

                            block = blocks.get(block.nextBlock);
                            if (block != null) {

                                nextBlockIds.add(block.getStringId());

                            }

                        }
                        response.put("nextBlockIds", nextBlockIds);

                    }
                    break;

                    case "getNextBlocks":
                    {

                        List<Block> nextBlocks = new ArrayList<>();
                        int totalLength = 0;
                        Block block = blocks.get(Convert.parseUnsignedLong((String) request.get("blockId")));
                        while (block != null) {

                            block = blocks.get(block.nextBlock);
                            if (block != null) {

                                int length = BLOCK_HEADER_LENGTH + block.payloadLength;
                                if (totalLength + length > 1048576) {

                                    break;

                                }

                                nextBlocks.add(block);
                                totalLength += length;

                            }

                        }

                        JSONArray nextBlocksArray = new JSONArray();
                        for (Block nextBlock : nextBlocks) {

                            nextBlocksArray.add(nextBlock.getJSONStreamAware());

                        }
                        response.put("nextBlocks", nextBlocksArray);

                    }
                    break;

                    case "getPeers":
                    {

                        JSONArray peers = new JSONArray();
                        for (Peer otherPeer : Nxt.peers.values()) {

                            if (otherPeer.blacklistingTime == 0 && otherPeer.announcedAddress.length() > 0 && otherPeer.state == Peer.STATE_CONNECTED && otherPeer.shareAddress) {

                                peers.add(otherPeer.announcedAddress);

                            }

                        }
                        response.put("peers", peers);

                    }
                    break;

                    case "getUnconfirmedTransactions":
                    {

                        JSONArray transactionsData = new JSONArray();
                        for (Transaction transaction : unconfirmedTransactions.values()) {

                            transactionsData.add(transaction.getJSONObject());

                        }
                        response.put("unconfirmedTransactions", transactionsData);

                    }
                    break;

                    case "processBlock":
                    {

                        boolean accepted;

                        Block block = Block.getBlock(request);

                        if (block == null) {

                            accepted = false;
                            if (peer != null) {
                                peer.blacklist();
                            }

                        } else {

                            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_HEADER_LENGTH + block.payloadLength);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);

                            buffer.put(block.getBytes());

                            JSONArray transactionsData = (JSONArray)request.get("transactions");
                            for (Object transaction : transactionsData) {

                                buffer.put(Transaction.getTransaction((JSONObject)transaction).getBytes());

                            }

                            accepted = Block.pushBlock(buffer, true);

                        }
                        response.put("accepted", accepted);

                    }
                    break;

                    case "processTransactions":
                    {

                        Transaction.processTransactions(request, "transactions");

                    }
                    break;

                    default:
                    {

                        response.put("error", "Unsupported request type!");

                    }

                }

            } else {

                Logger.logDebugMessage("Unsupported protocol " + request.get("protocol"));
                response.put("error", "Unsupported protocol!");

            }

        } catch (RuntimeException e) {
            Logger.logDebugMessage("Error processing POST request", e);
            response.put("error", e.toString());
        }

        resp.setContentType("text/plain; charset=UTF-8");

        CountingOutputStream cos = new CountingOutputStream(resp.getOutputStream());
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"))) {
            response.writeJSONString(writer);
        }

        if (peer != null) {
            peer.updateUploadedVolume(cos.getCount());
        }

    }

    @Override
    public void destroy() {

        shutdownExecutor(scheduledThreadPool);
        shutdownExecutor(sendToPeersService);

        try {
            Block.saveBlocks("blocks.nxt", true);
            Logger.logMessage("Saved blocks.nxt");
        } catch (RuntimeException e) {
            Logger.logMessage("Error saving blocks", e);
        }

        try {
            Transaction.saveTransactions("transactions.nxt");
            Logger.logMessage("Saved transactions.nxt");
        } catch (RuntimeException e) {
            Logger.logMessage("Error saving transactions", e);
        }

        /* no longer used
        try {

            blockchainChannel.close();

        } catch (Exception e) { }
        */

        Logger.logMessage("NRS " + Nxt.VERSION + " stopped.");

    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! executor.isTerminated()) {
            Logger.logMessage("some threads didn't terminate, forcing shutdown");
            executor.shutdownNow();
        }
    }

}
