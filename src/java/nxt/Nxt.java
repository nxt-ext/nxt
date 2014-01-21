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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class Nxt extends HttpServlet {

    public static final String VERSION = "0.6.0";

    public static final int BLOCK_HEADER_LENGTH = 224;
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 128;
    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;

    public static final int ALIAS_SYSTEM_BLOCK = 22000;
    public static final int TRANSPARENT_FORGING_BLOCK = 30000;
    public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;

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
    static /*final*/ int blacklistingPeriod;

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static /*final*/ int communicationLoggingMask;

    static final AtomicInteger transactionCounter = new AtomicInteger();
    static final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap<>();

    static /*final*/ Set<String> wellKnownPeers;
    static /*final*/ int maxNumberOfConnectedPublicPeers;
    static /*final*/ int connectTimeout;
    static int readTimeout;
    static /*final*/ boolean enableHallmarkProtection;
    static /*final*/ int pushThreshold;
    static int pullThreshold;
    static /*final*/ int sendToPeersLimit;
    static final AtomicInteger peerCounter = new AtomicInteger();
    static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();

    static final AtomicInteger blockCounter = new AtomicInteger();
    static final ConcurrentMap<Long, Block> blocks = new ConcurrentHashMap<>();
    static final AtomicReference<Block> lastBlock = new AtomicReference<>();
    static volatile Peer lastBlockchainFeeder;

    static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();

    static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap<>();
    static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap<>();

    static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap<>();
    static final ConcurrentMap<String, Long> assetNameToIdMappings = new ConcurrentHashMap<>();

    static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

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

            Blockchain.init();

            ThreadPools.start();

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

                                                                    int timestamp = Convert.getEpochTime();

                                                                    Transaction transaction = new Transaction(Transaction.TYPE_MESSAGING, Transaction.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT, timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
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

                                    byte[] publicKeyHash = Crypto.sha256().digest(Crypto.getPublicKey(secretPhrase));
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

                                response.put("genesisBlockId", Convert.convert(Genesis.GENESIS_BLOCK_ID));
                                response.put("genesisAccountId", Convert.convert(Genesis.CREATOR_ID));
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
                                response.put("time", Convert.getEpochTime());
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
                                response.put("numberOfOrders", Blockchain.askOrders.size() + Blockchain.bidOrders.size());
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

                                response.put("time", Convert.getEpochTime());

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

                                                                        int timestamp = Convert.getEpochTime();

                                                                        Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE, timestamp, (short)1440, publicKey, Genesis.CREATOR_ID, 0, fee, 0);
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

                                                AskOrder orderData = Blockchain.askOrders.get(order);
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

                                                            int timestamp = Convert.getEpochTime();

                                                            Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION, timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
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

                                                BidOrder orderData = Blockchain.bidOrders.get(order);
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

                                                            int timestamp = Convert.getEpochTime();

                                                            Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION, timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
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

                                                                    int timestamp = Convert.getEpochTime();

                                                                    Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASSET_TRANSFER, timestamp, deadline, publicKey, recipient, 0, fee, referencedTransaction);
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

                                                                    int timestamp = Convert.getEpochTime();

                                                                    Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT, timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
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

                                                                int timestamp = Convert.getEpochTime();

                                                                Transaction transaction = new Transaction(Transaction.TYPE_COLORED_COINS, Transaction.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT, timestamp, deadline, publicKey, Genesis.CREATOR_ID, 0, fee, referencedTransaction);
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
                                            for (AskOrder askOrder : Blockchain.askOrders.values()) {

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
                                            for (BidOrder bidOrder : Blockchain.bidOrders.values()) {

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

                                        AskOrder orderData = Blockchain.askOrders.get(Convert.parseUnsignedLong(order));
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
                                for (Long orderId : Blockchain.askOrders.keySet()) {

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

                                        BidOrder orderData = Blockchain.bidOrders.get(Convert.parseUnsignedLong(order));
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
                                for (Long orderId : Blockchain.bidOrders.keySet()) {

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

                                                            int timestamp = Convert.getEpochTime();

                                                            Transaction transaction = new Transaction(Transaction.TYPE_MESSAGING, Transaction.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE, timestamp, deadline, publicKey, recipient, 0, fee, referencedTransaction);
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

                                                            Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, Convert.getEpochTime(), deadline, publicKey, recipient, amount, fee, referencedTransaction);
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
                    int timestamp = Convert.getEpochTime();
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

                        if (blockId == Genesis.GENESIS_BLOCK_ID) {

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

                                final Transaction transaction = new Transaction(Transaction.TYPE_PAYMENT, Transaction.SUBTYPE_PAYMENT_ORDINARY_PAYMENT, Convert.getEpochTime(), deadline, user.publicKey, recipient, amount, fee, 0);
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
                            MessageDigest digest = Crypto.sha256();
                            byte[] generationSignatureHash;
                            if (lastBlock.height < TRANSPARENT_FORGING_BLOCK) {

                                byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
                                generationSignatureHash = digest.digest(generationSignature);

                            } else {

                                digest.update(lastBlock.generationSignature);
                                generationSignatureHash = digest.digest(user.publicKey);

                            }
                            BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
                            response2.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (Convert.getEpochTime() - lastBlock.timestamp));

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

                            if (blockId == Genesis.GENESIS_BLOCK_ID) {

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

                            accepted = Blockchain.pushBlock(buffer, true);

                        }
                        response.put("accepted", accepted);

                    }
                    break;

                    case "processTransactions":
                    {

                        Blockchain.processTransactions(request, "transactions");

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

        ThreadPools.shutdown();

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

}
