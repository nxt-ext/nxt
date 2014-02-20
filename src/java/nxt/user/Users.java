package nxt.user;

import nxt.Account;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Generator;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Users {

    private static final int DEFAULT_UI_PORT = 7875;

    private static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());

    private static final AtomicInteger peerCounter = new AtomicInteger();
    private static final ConcurrentMap<String, Integer> peerIndexMap = new ConcurrentHashMap<>();

    private static final AtomicInteger blockCounter = new AtomicInteger();
    private static final ConcurrentMap<Long, Integer> blockIndexMap = new ConcurrentHashMap<>();

    private static final AtomicInteger transactionCounter = new AtomicInteger();
    private static final ConcurrentMap<Long, Integer> transactionIndexMap = new ConcurrentHashMap<>();

    static final Set<String> allowedUserHosts;

    static {

        String allowedUserHostsString = Nxt.getStringProperty("nxt.allowedUserHosts", "127.0.0.1; localhost; 0:0:0:0:0:0:0:1;");
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

        boolean enableUIServer = Nxt.getBooleanProperty("nxt.enableUIServer", allowedUserHosts == null || !allowedUserHosts.isEmpty());
        if (enableUIServer) {
            try {
                int port = Nxt.getIntProperty("nxt.UIServerPort", Users.DEFAULT_UI_PORT);
                Server userServer = new Server(Users.DEFAULT_UI_PORT);

                ServletHandler userHandler = new ServletHandler();
                ServletHolder userHolder = userHandler.addServletWithMapping(UserServlet.class, "/nxt");
                userHolder.setAsyncSupported(true);

                ResourceHandler userFileHandler = new ResourceHandler();
                userFileHandler.setDirectoriesListed(false);
                userFileHandler.setWelcomeFiles(new String[]{"index.html"});
                userFileHandler.setResourceBase(Nxt.getStringProperty("nxt.uiResourceBase", "html/nrs"));

                HandlerList userHandlers = new HandlerList();
                userHandlers.setHandlers(new Handler[] { userFileHandler, userHandler, new DefaultHandler() });

                userServer.setHandler(userHandlers);
                userServer.start();
                Logger.logMessage("Started user interface server on port " + port);
            } catch (Exception e) {
                Logger.logDebugMessage("Failed to start user interface server", e);
                throw new RuntimeException(e.toString(), e);
            }
        } else {
            Logger.logMessage("User interface server not enabled");
        }

    }

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                JSONObject response = new JSONObject();
                response.put("response", "setBalance");
                response.put("balance", account.getUnconfirmedBalance());
                byte[] accountPublicKey = account.getPublicKey();
                for (User user : Users.users.values()) {
                    if (user.getSecretPhrase() != null && Arrays.equals(user.getPublicKey(), accountPublicKey)) {
                        user.send(response);
                    }
                }
            }
        }, Account.Event.UNCONFIRMED_BALANCE);
    }

    static {
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedKnownPeers = new JSONArray();
                JSONObject removedKnownPeer = new JSONObject();
                removedKnownPeer.put("index", Users.getIndex(peer));
                removedKnownPeers.add(removedKnownPeer);
                response.put("removedKnownPeers", removedKnownPeers);
                JSONArray addedBlacklistedPeers = new JSONArray();
                JSONObject addedBlacklistedPeer = new JSONObject();
                addedBlacklistedPeer.put("index", Users.getIndex(peer));
                addedBlacklistedPeer.put("address", peer.getPeerAddress());
                addedBlacklistedPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                if (peer.isWellKnown()) {
                    addedBlacklistedPeer.put("wellKnown", true);
                }
                addedBlacklistedPeer.put("software", peer.getSoftware());
                addedBlacklistedPeers.add(addedBlacklistedPeer);
                response.put("addedBlacklistedPeers", addedBlacklistedPeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.BLACKLIST);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedActivePeers = new JSONArray();
                JSONObject removedActivePeer = new JSONObject();
                removedActivePeer.put("index", Users.getIndex(peer));
                removedActivePeers.add(removedActivePeer);
                response.put("removedActivePeers", removedActivePeers);
                JSONArray addedKnownPeers = new JSONArray();
                JSONObject addedKnownPeer = new JSONObject();
                addedKnownPeer.put("index", Users.getIndex(peer));
                addedKnownPeer.put("address", peer.getPeerAddress());
                addedKnownPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                if (peer.isWellKnown()) {
                    addedKnownPeer.put("wellKnown", true);
                }
                addedKnownPeer.put("software", peer.getSoftware());
                addedKnownPeers.add(addedKnownPeer);
                response.put("addedKnownPeers", addedKnownPeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.DEACTIVATE);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedBlacklistedPeers = new JSONArray();
                JSONObject removedBlacklistedPeer = new JSONObject();
                removedBlacklistedPeer.put("index", Users.getIndex(peer));
                removedBlacklistedPeers.add(removedBlacklistedPeer);
                response.put("removedBlacklistedPeers", removedBlacklistedPeers);
                JSONArray addedKnownPeers = new JSONArray();
                JSONObject addedKnownPeer = new JSONObject();
                addedKnownPeer.put("index", Users.getIndex(peer));
                addedKnownPeer.put("address", peer.getPeerAddress());
                addedKnownPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                if (peer.isWellKnown()) {
                    addedKnownPeer.put("wellKnown", true);
                }
                addedKnownPeer.put("software", peer.getSoftware());
                addedKnownPeers.add(addedKnownPeer);
                response.put("addedKnownPeers", addedKnownPeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.UNBLACKLIST);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedKnownPeers = new JSONArray();
                JSONObject removedKnownPeer = new JSONObject();
                removedKnownPeer.put("index", Users.getIndex(peer));
                removedKnownPeers.add(removedKnownPeer);
                response.put("removedKnownPeers", removedKnownPeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.REMOVE);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", Users.getIndex(peer));
                changedActivePeer.put("downloaded", peer.getDownloadedVolume());
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.DOWNLOADED_VOLUME);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", Users.getIndex(peer));
                changedActivePeer.put("uploaded", peer.getUploadedVolume());
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.UPLOADED_VOLUME);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", Users.getIndex(peer));
                changedActivePeer.put("weight", peer.getWeight());
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.WEIGHT);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedKnownPeers = new JSONArray();
                JSONObject removedKnownPeer = new JSONObject();
                removedKnownPeer.put("index", Users.getIndex(peer));
                removedKnownPeers.add(removedKnownPeer);
                response.put("removedKnownPeers", removedKnownPeers);
                JSONArray addedActivePeers = new JSONArray();
                JSONObject addedActivePeer = new JSONObject();
                addedActivePeer.put("index", Users.getIndex(peer));
                if (peer.getState() == Peer.State.DISCONNECTED) {
                    addedActivePeer.put("disconnected", true);
                }
                addedActivePeer.put("address", Convert.truncate(peer.getPeerAddress(), "-", 25, true));
                addedActivePeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                if (peer.isWellKnown()) {
                    addedActivePeer.put("wellKnown", true);
                }
                addedActivePeer.put("weight", peer.getWeight());
                addedActivePeer.put("downloaded", peer.getDownloadedVolume());
                addedActivePeer.put("uploaded", peer.getUploadedVolume());
                addedActivePeer.put("software", peer.getSoftware());
                addedActivePeers.add(addedActivePeer);
                response.put("addedActivePeers", addedActivePeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.ADDED_ACTIVE_PEER);

        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", Users.getIndex(peer));
                changedActivePeer.put(peer.getState() == Peer.State.CONNECTED ? "connected" : "disconnected", true);
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                Users.sendNewDataToAll(response);
            }
        }, Peers.Event.CHANGED_ACTIVE_PEER);

    }

    static {

        Nxt.getTransactionProcessor().addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray removedUnconfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject removedUnconfirmedTransaction = new JSONObject();
                    removedUnconfirmedTransaction.put("index", Users.getIndex(transaction));
                    removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);
                }
                response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);
                Users.sendNewDataToAll(response);
            }
        }, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);

        Nxt.getTransactionProcessor().addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray addedUnconfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject addedUnconfirmedTransaction = new JSONObject();
                    addedUnconfirmedTransaction.put("index", Users.getIndex(transaction));
                    addedUnconfirmedTransaction.put("timestamp", transaction.getTimestamp());
                    addedUnconfirmedTransaction.put("deadline", transaction.getDeadline());
                    addedUnconfirmedTransaction.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
                    addedUnconfirmedTransaction.put("amount", transaction.getAmount());
                    addedUnconfirmedTransaction.put("fee", transaction.getFee());
                    addedUnconfirmedTransaction.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
                    addedUnconfirmedTransaction.put("id", transaction.getStringId());
                    addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);
                }
                response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);
                Users.sendNewDataToAll(response);
            }
        }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);

        Nxt.getTransactionProcessor().addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray addedConfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject addedConfirmedTransaction = new JSONObject();
                    addedConfirmedTransaction.put("index", Users.getIndex(transaction));
                    addedConfirmedTransaction.put("blockTimestamp", transaction.getBlock().getTimestamp());
                    addedConfirmedTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    addedConfirmedTransaction.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
                    addedConfirmedTransaction.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
                    addedConfirmedTransaction.put("amount", transaction.getAmount());
                    addedConfirmedTransaction.put("fee", transaction.getFee());
                    addedConfirmedTransaction.put("id", transaction.getStringId());
                    addedConfirmedTransactions.add(addedConfirmedTransaction);
                }
                response.put("addedConfirmedTransactions", addedConfirmedTransactions);
                Users.sendNewDataToAll(response);
            }
        }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);

        Nxt.getTransactionProcessor().addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray newTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject newTransaction = new JSONObject();
                    newTransaction.put("index", Users.getIndex(transaction));
                    newTransaction.put("timestamp", transaction.getTimestamp());
                    newTransaction.put("deadline", transaction.getDeadline());
                    newTransaction.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
                    newTransaction.put("amount", transaction.getAmount());
                    newTransaction.put("fee", transaction.getFee());
                    newTransaction.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
                    newTransaction.put("id", transaction.getStringId());
                    newTransactions.add(newTransaction);
                }
                response.put("addedDoubleSpendingTransactions", newTransactions);
                Users.sendNewDataToAll(response);
            }
        }, TransactionProcessor.Event.ADDED_DOUBLESPENDING_TRANSACTIONS);

        Nxt.getBlockchainProcessor().addBlockListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                JSONObject response = new JSONObject();
                JSONArray addedOrphanedBlocks = new JSONArray();
                JSONObject addedOrphanedBlock = new JSONObject();
                addedOrphanedBlock.put("index", Users.getIndex(block));
                addedOrphanedBlock.put("timestamp", block.getTimestamp());
                addedOrphanedBlock.put("numberOfTransactions", block.getTransactionIds().size());
                addedOrphanedBlock.put("totalAmount", block.getTotalAmount());
                addedOrphanedBlock.put("totalFee", block.getTotalFee());
                addedOrphanedBlock.put("payloadLength", block.getPayloadLength());
                addedOrphanedBlock.put("generator", Convert.toUnsignedLong(block.getGeneratorId()));
                addedOrphanedBlock.put("height", block.getHeight());
                addedOrphanedBlock.put("version", block.getVersion());
                addedOrphanedBlock.put("block", block.getStringId());
                addedOrphanedBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
                addedOrphanedBlocks.add(addedOrphanedBlock);
                response.put("addedOrphanedBlocks", addedOrphanedBlocks);
                Users.sendNewDataToAll(response);
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);

        Nxt.getBlockchainProcessor().addBlockListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                JSONObject response = new JSONObject();
                JSONArray addedRecentBlocks = new JSONArray();
                JSONObject addedRecentBlock = new JSONObject();
                addedRecentBlock.put("index", Users.getIndex(block));
                addedRecentBlock.put("timestamp", block.getTimestamp());
                addedRecentBlock.put("numberOfTransactions", block.getTransactionIds().size());
                addedRecentBlock.put("totalAmount", block.getTotalAmount());
                addedRecentBlock.put("totalFee", block.getTotalFee());
                addedRecentBlock.put("payloadLength", block.getPayloadLength());
                addedRecentBlock.put("generator", Convert.toUnsignedLong(block.getGeneratorId()));
                addedRecentBlock.put("height", block.getHeight());
                addedRecentBlock.put("version", block.getVersion());
                addedRecentBlock.put("block", block.getStringId());
                addedRecentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
                addedRecentBlocks.add(addedRecentBlock);
                response.put("addedRecentBlocks", addedRecentBlocks);
                Users.sendNewDataToAll(response);
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);

    }

    static {
        Generator.addListener(new Listener<Generator>() {
            @Override
            public void notify(Generator generator) {
                JSONObject response = new JSONObject();
                response.put("response", "setBlockGenerationDeadline");
                response.put("deadline", generator.getDeadline());
                for (User user : users.values()) {
                    if (Arrays.equals(generator.getPublicKey(), user.getPublicKey())) {
                        user.send(response);
                    }
                }
            }
        }, Generator.Event.GENERATION_DEADLINE);
    }

    static Collection<User> getAllUsers() {
        return allUsers;
    }

    static User getUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            user = new User(userId);
            User oldUser = users.putIfAbsent(userId, user);
            if (oldUser != null) {
                user = oldUser;
                user.setInactive(false);
            }
        } else {
            user.setInactive(false);
        }
        return user;
    }

    static User remove(User user) {
        return users.remove(user.getUserId());
    }

    private static void sendNewDataToAll(JSONObject response) {
        response.put("response", "processNewData");
        sendToAll(response);
    }

    private static void sendToAll(JSONStreamAware response) {
        for (User user : users.values()) {
            user.send(response);
        }
    }

    static int getIndex(Peer peer) {
        Integer index = peerIndexMap.get(peer.getPeerAddress());
        if (index == null) {
            index = peerCounter.incrementAndGet();
            peerIndexMap.put(peer.getPeerAddress(), index);
        }
        return index;
    }

    static int getIndex(Block block) {
        Integer index = blockIndexMap.get(block.getId());
        if (index == null) {
            index = blockCounter.incrementAndGet();
            blockIndexMap.put(block.getId(), index);
        }
        return index;
    }

    static int getIndex(Transaction transaction) {
        Integer index = transactionIndexMap.get(transaction.getId());
        if (index == null) {
            index = transactionCounter.incrementAndGet();
            transactionIndexMap.put(transaction.getId(), index);
        }
        return index;
    }

    public static void init() {}

    private Users() {} // never

}
