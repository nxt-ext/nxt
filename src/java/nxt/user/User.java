package nxt.user;

import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.Generator;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public final class User {

    private static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                JSONObject response = new JSONObject();
                response.put("response", "setBalance");
                response.put("balance", account.getUnconfirmedBalance());
                byte[] accountPublicKey = account.getPublicKey();
                for (User user : users.values()) {
                    if (user.getSecretPhrase() != null && Arrays.equals(user.getPublicKey(), accountPublicKey)) {
                        user.send(response);
                    }
                }
            }
        }, Account.Event.UNCONFIRMED_BALANCE);
    }

    static {
        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedKnownPeers = new JSONArray();
                JSONObject removedKnownPeer = new JSONObject();
                removedKnownPeer.put("index", peer.getIndex());
                removedKnownPeers.add(removedKnownPeer);
                response.put("removedKnownPeers", removedKnownPeers);
                JSONArray addedBlacklistedPeers = new JSONArray();
                JSONObject addedBlacklistedPeer = new JSONObject();
                addedBlacklistedPeer.put("index", peer.getIndex());
                addedBlacklistedPeer.put("address", peer.getPeerAddress());
                addedBlacklistedPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                if (peer.isWellKnown()) {
                    addedBlacklistedPeer.put("wellKnown", true);
                }
                addedBlacklistedPeer.put("software", peer.getSoftware());
                addedBlacklistedPeers.add(addedBlacklistedPeer);
                response.put("addedBlacklistedPeers", addedBlacklistedPeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.BLACKLIST);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedActivePeers = new JSONArray();
                JSONObject removedActivePeer = new JSONObject();
                removedActivePeer.put("index", peer.getIndex());
                removedActivePeers.add(removedActivePeer);
                response.put("removedActivePeers", removedActivePeers);
                if (peer.getAnnouncedAddress() != null) {
                    JSONArray addedKnownPeers = new JSONArray();
                    JSONObject addedKnownPeer = new JSONObject();
                    addedKnownPeer.put("index", peer.getIndex());
                    addedKnownPeer.put("address", peer.getPeerAddress());
                    addedKnownPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                    if (peer.isWellKnown()) {
                        addedKnownPeer.put("wellKnown", true);
                    }
                    addedKnownPeer.put("software", peer.getSoftware());
                    addedKnownPeers.add(addedKnownPeer);
                    response.put("addedKnownPeers", addedKnownPeers);
                }
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.DEACTIVATE);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedBlacklistedPeers = new JSONArray();
                JSONObject removedBlacklistedPeer = new JSONObject();
                removedBlacklistedPeer.put("index", peer.getIndex());
                removedBlacklistedPeers.add(removedBlacklistedPeer);
                response.put("removedBlacklistedPeers", removedBlacklistedPeers);
                JSONArray addedKnownPeers = new JSONArray();
                JSONObject addedKnownPeer = new JSONObject();
                addedKnownPeer.put("index", peer.getIndex());
                addedKnownPeer.put("address", peer.getPeerAddress());
                addedKnownPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                if (peer.isWellKnown()) {
                    addedKnownPeer.put("wellKnown", true);
                }
                addedKnownPeer.put("software", peer.getSoftware());
                addedKnownPeers.add(addedKnownPeer);
                response.put("addedKnownPeers", addedKnownPeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.UNBLACKLIST);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray removedKnownPeers = new JSONArray();
                JSONObject removedKnownPeer = new JSONObject();
                removedKnownPeer.put("index", peer.getIndex());
                removedKnownPeers.add(removedKnownPeer);
                response.put("removedKnownPeers", removedKnownPeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.REMOVE);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", peer.getIndex());
                changedActivePeer.put("downloaded", peer.getDownloadedVolume());
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.DOWNLOADED_VOLUME);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", peer.getIndex());
                changedActivePeer.put("uploaded", peer.getUploadedVolume());
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.UPLOADED_VOLUME);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", peer.getIndex());
                changedActivePeer.put("weight", peer.getWeight());
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.WEIGHT);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                if (peer.getAnnouncedAddress() != null) {
                    JSONArray removedKnownPeers = new JSONArray();
                    JSONObject removedKnownPeer = new JSONObject();
                    removedKnownPeer.put("index", peer.getIndex());
                    removedKnownPeers.add(removedKnownPeer);
                    response.put("removedKnownPeers", removedKnownPeers);
                }
                JSONArray addedActivePeers = new JSONArray();
                JSONObject addedActivePeer = new JSONObject();
                addedActivePeer.put("index", peer.getIndex());
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
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.ADDED_ACTIVE_PEER);

        Peer.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                JSONObject response = new JSONObject();
                JSONArray changedActivePeers = new JSONArray();
                JSONObject changedActivePeer = new JSONObject();
                changedActivePeer.put("index", peer.getIndex());
                changedActivePeer.put(peer.getState() == Peer.State.CONNECTED ? "connected" : "disconnected", true);
                changedActivePeers.add(changedActivePeer);
                response.put("changedActivePeers", changedActivePeers);
                User.sendNewDataToAll(response);
            }
        }, Peer.Event.CHANGED_ACTIVE_PEER);

    }

    static {

        Blockchain.addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray removedUnconfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject removedUnconfirmedTransaction = new JSONObject();
                    removedUnconfirmedTransaction.put("index", transaction.getIndex());
                    removedUnconfirmedTransactions.add(removedUnconfirmedTransaction);
                }
                response.put("removedUnconfirmedTransactions", removedUnconfirmedTransactions);
                User.sendNewDataToAll(response);
            }
        }, Blockchain.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);

        Blockchain.addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray addedUnconfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject addedUnconfirmedTransaction = new JSONObject();
                    addedUnconfirmedTransaction.put("index", transaction.getIndex());
                    addedUnconfirmedTransaction.put("timestamp", transaction.getTimestamp());
                    addedUnconfirmedTransaction.put("deadline", transaction.getDeadline());
                    addedUnconfirmedTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
                    addedUnconfirmedTransaction.put("amount", transaction.getAmount());
                    addedUnconfirmedTransaction.put("fee", transaction.getFee());
                    addedUnconfirmedTransaction.put("sender", Convert.convert(transaction.getSenderId()));
                    addedUnconfirmedTransaction.put("id", transaction.getStringId());
                    addedUnconfirmedTransactions.add(addedUnconfirmedTransaction);
                }
                response.put("addedUnconfirmedTransactions", addedUnconfirmedTransactions);
                User.sendNewDataToAll(response);
            }
        }, Blockchain.Event.ADDED_UNCONFIRMED_TRANSACTIONS);

        Blockchain.addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray addedConfirmedTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject addedConfirmedTransaction = new JSONObject();
                    addedConfirmedTransaction.put("index", transaction.getIndex());
                    addedConfirmedTransaction.put("blockTimestamp", transaction.getBlock().getTimestamp());
                    addedConfirmedTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    addedConfirmedTransaction.put("sender", Convert.convert(transaction.getSenderId()));
                    addedConfirmedTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
                    addedConfirmedTransaction.put("amount", transaction.getAmount());
                    addedConfirmedTransaction.put("fee", transaction.getFee());
                    addedConfirmedTransaction.put("id", transaction.getStringId());
                    addedConfirmedTransactions.add(addedConfirmedTransaction);
                }
                response.put("addedConfirmedTransactions", addedConfirmedTransactions);
                User.sendNewDataToAll(response);
            }
        }, Blockchain.Event.ADDED_CONFIRMED_TRANSACTIONS);

        Blockchain.addTransactionListener(new Listener<List<Transaction>>() {
            @Override
            public void notify(List<Transaction> transactions) {
                JSONObject response = new JSONObject();
                JSONArray newTransactions = new JSONArray();
                for (Transaction transaction : transactions) {
                    JSONObject newTransaction = new JSONObject();
                    newTransaction.put("index", transaction.getIndex());
                    newTransaction.put("timestamp", transaction.getTimestamp());
                    newTransaction.put("deadline", transaction.getDeadline());
                    newTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
                    newTransaction.put("amount", transaction.getAmount());
                    newTransaction.put("fee", transaction.getFee());
                    newTransaction.put("sender", Convert.convert(transaction.getSenderId()));
                    newTransaction.put("id", transaction.getStringId());
                    newTransactions.add(newTransaction);
                }
                response.put("addedDoubleSpendingTransactions", newTransactions);
                User.sendNewDataToAll(response);
            }
        }, Blockchain.Event.ADDED_DOUBLESPENDING_TRANSACTIONS);

        Blockchain.addBlockListener(new Listener<List<Block>>() {
            @Override
            public void notify(List<Block> blocks) {
                JSONObject response = new JSONObject();
                JSONArray addedOrphanedBlocks = new JSONArray();
                for (Block block : blocks) {
                    JSONObject addedOrphanedBlock = new JSONObject();
                    addedOrphanedBlock.put("index", block.getIndex());
                    addedOrphanedBlock.put("timestamp", block.getTimestamp());
                    addedOrphanedBlock.put("numberOfTransactions", block.getTransactionIds().length);
                    addedOrphanedBlock.put("totalAmount", block.getTotalAmount());
                    addedOrphanedBlock.put("totalFee", block.getTotalFee());
                    addedOrphanedBlock.put("payloadLength", block.getPayloadLength());
                    addedOrphanedBlock.put("generator", Convert.convert(block.getGeneratorId()));
                    addedOrphanedBlock.put("height", block.getHeight());
                    addedOrphanedBlock.put("version", block.getVersion());
                    addedOrphanedBlock.put("block", block.getStringId());
                    addedOrphanedBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
                    addedOrphanedBlocks.add(addedOrphanedBlock);
                }
                response.put("addedOrphanedBlocks", addedOrphanedBlocks);
                User.sendNewDataToAll(response);
            }
        }, Blockchain.Event.BLOCK_POPPED);

        Blockchain.addBlockListener(new Listener<List<Block>>() {
            @Override
            public void notify(List<Block> blocks) {
                JSONObject response = new JSONObject();
                JSONArray addedRecentBlocks = new JSONArray();
                for (Block block : blocks) {
                    JSONObject addedRecentBlock = new JSONObject();
                    addedRecentBlock.put("index", block.getIndex());
                    addedRecentBlock.put("timestamp", block.getTimestamp());
                    addedRecentBlock.put("numberOfTransactions", block.getTransactionIds().length);
                    addedRecentBlock.put("totalAmount", block.getTotalAmount());
                    addedRecentBlock.put("totalFee", block.getTotalFee());
                    addedRecentBlock.put("payloadLength", block.getPayloadLength());
                    addedRecentBlock.put("generator", Convert.convert(block.getGeneratorId()));
                    addedRecentBlock.put("height", block.getHeight());
                    addedRecentBlock.put("version", block.getVersion());
                    addedRecentBlock.put("block", block.getStringId());
                    addedRecentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000)).divide(BigInteger.valueOf(Nxt.initialBaseTarget)));
                    addedRecentBlocks.add(addedRecentBlock);
                }
                response.put("addedRecentBlocks", addedRecentBlocks);
                User.sendNewDataToAll(response);
            }
        }, Blockchain.Event.BLOCK_PUSHED);

    }

    static {
        Generator.addListener(new Listener<Generator>() {
            @Override
            public void notify(Generator generator) {
                JSONObject response = new JSONObject();
                response.put("response", "setBlockGenerationDeadline");
                response.put("deadline", generator.getDeadline());
                for (User user : allUsers) {
                    if (Arrays.equals(generator.getPublicKey(), user.getPublicKey())) {
                        user.send(response);
                    }
                }
            }
        }, Generator.Event.GENERATION_DEADLINE);
    }

    public static Collection<User> getAllUsers() {
        return allUsers;
    }

    public static User getUser(String userPasscode) {
        User user = users.get(userPasscode);
        if (user == null) {
            user = new User();
            User oldUser = users.putIfAbsent(userPasscode, user);
            if (oldUser != null) {
                user = oldUser;
                user.isInactive = false;
            }
        } else {
            user.isInactive = false;
        }
        return user;
    }

    private static void sendNewDataToAll(JSONObject response) {
        response.put("response", "processNewData");
        sendToAll(response);
    }

    private static void sendToAll(JSONStreamAware response) {
        for (User user : User.users.values()) {
            user.send(response);
        }
    }

    private volatile String secretPhrase;
    private volatile byte[] publicKey;
    private volatile boolean isInactive;
    private final ConcurrentLinkedQueue<JSONStreamAware> pendingResponses = new ConcurrentLinkedQueue<>();
    private AsyncContext asyncContext;

    User() {}

    public byte[] getPublicKey() {
        return publicKey;
    }

    String getSecretPhrase() {
        return secretPhrase;
    }

    boolean isInactive() {
        return isInactive;
    }

    void enqueue(JSONStreamAware response) {
        pendingResponses.offer(response);
    }

    void lockAccount() {
        Generator.stopForging(secretPhrase);
        secretPhrase = null;
    }

    Long unlockAccount(String secretPhrase) {
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.secretPhrase = secretPhrase;
        Generator.startForging(secretPhrase, publicKey);
        return Account.getId(publicKey);
    }

    public synchronized void processPendingResponses(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONArray responses = new JSONArray();
        JSONStreamAware pendingResponse;
        while ((pendingResponse = pendingResponses.poll()) != null) {
            responses.add(pendingResponse);
        }
        if (responses.size() > 0) {
            JSONObject combinedResponse = new JSONObject();
            combinedResponse.put("responses", responses);
            if (asyncContext != null) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    combinedResponse.writeJSONString(writer);
                }
                asyncContext.complete();
                asyncContext = req.startAsync();
                asyncContext.addListener(new UserAsyncListener());
                asyncContext.setTimeout(5000);
            } else {
                resp.setContentType("text/plain; charset=UTF-8");
                try (Writer writer = resp.getWriter()) {
                    combinedResponse.writeJSONString(writer);
                }
            }
        } else {
            if (asyncContext != null) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    JSON.emptyJSON.writeJSONString(writer);
                }
                asyncContext.complete();
            }
            asyncContext = req.startAsync();
            asyncContext.addListener(new UserAsyncListener());
            asyncContext.setTimeout(5000);
        }
    }

    private synchronized void send(JSONStreamAware response) {
        if (asyncContext == null) {

            if (isInactive) {
                // user not seen recently, no responses should be collected
                return;
            }
            if (pendingResponses.size() > 1000) {
                pendingResponses.clear();
                // stop collecting responses for this user
                isInactive = true;
                if (secretPhrase == null) {
                    // but only completely remove users that don't have unlocked accounts
                    users.values().remove(this);
                }
                return;
            }

            pendingResponses.offer(response);

        } else {

            JSONArray responses = new JSONArray();
            JSONStreamAware pendingResponse;
            while ((pendingResponse = pendingResponses.poll()) != null) {

                responses.add(pendingResponse);

            }
            responses.add(response);

            JSONObject combinedResponse = new JSONObject();
            combinedResponse.put("responses", responses);

            asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

            try (Writer writer = asyncContext.getResponse().getWriter()) {
                combinedResponse.writeJSONString(writer);
            } catch (IOException e) {
                Logger.logMessage("Error sending response to user", e);
            }

            asyncContext.complete();
            asyncContext = null;

        }

    }


    private final class UserAsyncListener implements AsyncListener {

        @Override
        public void onComplete(AsyncEvent asyncEvent) throws IOException { }

        @Override
        public void onError(AsyncEvent asyncEvent) throws IOException {

            synchronized (User.this) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    JSON.emptyJSON.writeJSONString(writer);
                }

                asyncContext.complete();
                asyncContext = null;
            }

        }

        @Override
        public void onStartAsync(AsyncEvent asyncEvent) throws IOException { }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {

            synchronized (User.this) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    JSON.emptyJSON.writeJSONString(writer);
                }

                asyncContext.complete();
                asyncContext = null;
            }

        }

    }

}
