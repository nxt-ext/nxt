package nxt.peer;

import nxt.Account;
import nxt.Nxt;
import nxt.NxtException;
import nxt.ThreadPools;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.user.User;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class Peer implements Comparable<Peer> {

    public static enum State {
        NON_CONNECTED, CONNECTED, DISCONNECTED
    }

    private static final AtomicInteger peerCounter = new AtomicInteger();
    private static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();
    private static final Collection<Peer> allPeers = Collections.unmodifiableCollection(peers.values());

    public static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {

                if (Peer.getNumberOfConnectedPublicPeers() < Nxt.maxNumberOfConnectedPublicPeers) {

                    Peer peer = Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? State.NON_CONNECTED : State.DISCONNECTED, false);
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

    };

    public static final Runnable peerUnBlacklistingThread = new Runnable() {

        @Override
        public void run() {

            try {

                long curTime = System.currentTimeMillis();

                for (Peer peer : peers.values()) {

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

    };

    public static final Runnable getMorePeersThread = new Runnable() {

        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            getPeersRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {

            try {

                Peer peer = Peer.getAnyPeer(State.CONNECTED, true);
                if (peer != null) {
                    JSONObject response = peer.send(getPeersRequest);
                    if (response != null) {
                        JSONArray peers = (JSONArray)response.get("peers");
                        for (Object peerAddress : peers) {
                            String address = ((String)peerAddress).trim();
                            if (address.length() > 0) {
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

    };

    public static Collection<Peer> getAllPeers() {
        return allPeers;
    }

    public static Peer getPeer(String peerAddress) {
        return peers.get(peerAddress);
    }

    public static Peer addPeer(String address, String announcedAddress) {

        try {
            new URL("http://" + address);
        } catch (MalformedURLException e) {
            Logger.logDebugMessage("malformed peer address " + address, e);
            return null;
        }

        try {
            new URL("http://" + announcedAddress);
        } catch (MalformedURLException e) {
            Logger.logDebugMessage("malformed peer announced address " + announcedAddress, e);
            announcedAddress = "";
        }

        if (address.equals("localhost") || address.equals("127.0.0.1") || address.equals("0:0:0:0:0:0:0:1")) {
            return null;
        }

        if (Nxt.myAddress != null && Nxt.myAddress.length() > 0 && Nxt.myAddress.equalsIgnoreCase(announcedAddress)) {
            return null;
        }

        Peer peer = peers.get(announcedAddress.length() > 0 ? announcedAddress : address);
        if (peer == null) {
            String peerAddress = announcedAddress.length() > 0 ? announcedAddress : address;
            peer = new Peer(peerAddress, announcedAddress);
            peers.put(peerAddress, peer);
        }

        return peer;
    }

    public static void updatePeerWeights(Account account) {
        for (Peer peer : peers.values()) {
            if (account.getId().equals(peer.accountId) && peer.adjustedWeight > 0) {
                peer.updateWeight();
            }
        }
    }

    public static void sendToSomePeers(final JSONObject request) {

        final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

        int successful = 0;
        List<Future<JSONObject>> expectedResponses = new ArrayList<>();
        for (final Peer peer : peers.values()) {

            if (Nxt.enableHallmarkProtection && peer.getWeight() < Nxt.pushThreshold) {
                continue;
            }

            if (peer.blacklistingTime == 0 && peer.state == State.CONNECTED && peer.announcedAddress.length() > 0) {
                Future<JSONObject> futureResponse = ThreadPools.sendInParallel(peer, jsonRequest);
                expectedResponses.add(futureResponse);
            }
            if (expectedResponses.size() >= Nxt.sendToPeersLimit - successful) {
                for (Future<JSONObject> future : expectedResponses) {
                    try {
                        JSONObject response = future.get();
                        if (response != null && response.get("error") == null) {
                            successful += 1;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Logger.logDebugMessage("Error in sendToSomePeers", e);
                    }

                }
                expectedResponses.clear();
            }
            if (successful >= Nxt.sendToPeersLimit) {
                return;
            }

        }

    }

    public static Peer getAnyPeer(State state, boolean applyPullThreshold) {

        List<Peer> selectedPeers = new ArrayList<Peer>();
        for (Peer peer : peers.values()) {
            if (peer.blacklistingTime <= 0 && peer.state == state && peer.announcedAddress.length() > 0
                    && (!applyPullThreshold || !Nxt.enableHallmarkProtection || peer.getWeight() >= Nxt.pullThreshold)) {
                selectedPeers.add(peer);
            }
        }

        if (selectedPeers.size() > 0) {
            long totalWeight = 0;
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                totalWeight += weight;
            }

            long hit = ThreadLocalRandom.current().nextLong(totalWeight);
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                if ((hit -= weight) < 0) {
                    return peer;
                }
            }
        }
        return null;
    }

    private static int getNumberOfConnectedPublicPeers() {
        int numberOfConnectedPeers = 0;
        for (Peer peer : peers.values()) {
            if (peer.state == State.CONNECTED && peer.announcedAddress.length() > 0) {
                numberOfConnectedPeers++;
            }
        }
        return numberOfConnectedPeers;
    }

    private static String truncate(String s, int limit, boolean dots) {
        return s == null ? "?" : s.length() > limit ? (s.substring(0, limit) + (dots ? "..." : "")) : s;
    }


    private final int index;
    private final String peerAddress;
    private String announcedAddress;
    private boolean shareAddress;
    private String hallmark;
    private String platform;
    private String application;
    private String version;
    private int weight;
    private int date;
    private Long accountId;
    private long adjustedWeight;
    private volatile long blacklistingTime;
    private volatile State state;
    private volatile long downloadedVolume;
    private volatile long uploadedVolume;

    private Peer(String peerAddress, String announcedAddress) {

        this.peerAddress = peerAddress;
        this.announcedAddress = announcedAddress;
        this.index = peerCounter.incrementAndGet();
        this.state = State.NON_CONNECTED;
    }

    public int getIndex() {
        return index;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public State getState() {
        return state;
    }

    public long getBlacklistingTime() {
        return blacklistingTime;
    }

    public long getDownloadedVolume() {
        return downloadedVolume;
    }

    public long getUploadedVolume() {
        return uploadedVolume;
    }

    public String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    public String getApplication() {
        return application;
    }

    void setApplication(String application) {
        this.application = application;
    }

    public String getPlatform() {
        return platform;
    }

    void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getHallmark() {
        return hallmark;
    }

    public boolean shareAddress() {
        return shareAddress;
    }

    void setShareAddress(boolean shareAddress) {
        this.shareAddress = shareAddress;
    }

    public String getAnnouncedAddress() {
        return announcedAddress;
    }

    void setAnnouncedAddress(String announcedAddress) {
        this.announcedAddress = announcedAddress;
    }

    @Override
    public int compareTo(Peer o) {
        long weight = getWeight(), weight2 = o.getWeight();
        if (weight > weight2) {
            return -1;
        } else if (weight < weight2) {
            return 1;
        } else {
            return index - o.index;
        }
    }

    public void blacklist(NxtException.ValidationException cause) {
        if (cause instanceof Transaction.NotYetEnabledException) {
            // don't blacklist peers just because a feature is not yet enabled
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        Logger.logDebugMessage("Blacklisting " + peerAddress + " because of: " + cause.getMessage(), cause);
        blacklist();
    }

    public void blacklist() {

        blacklistingTime = System.currentTimeMillis();

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray removedKnownPeers = new JSONArray();
        JSONObject removedKnownPeer = new JSONObject();
        removedKnownPeer.put("index", index);
        removedKnownPeers.add(removedKnownPeer);
        response.put("removedKnownPeers", removedKnownPeers);

        JSONArray addedBlacklistedPeers = new JSONArray();
        JSONObject addedBlacklistedPeer = new JSONObject();
        addedBlacklistedPeer.put("index", index);
        addedBlacklistedPeer.put("announcedAddress", truncate(announcedAddress, 30, true));
        if (Nxt.wellKnownPeers.contains(announcedAddress)) {
            addedBlacklistedPeer.put("wellKnown", true);
        }
        addedBlacklistedPeers.add(addedBlacklistedPeer);
        response.put("addedBlacklistedPeers", addedBlacklistedPeers);

        User.sendToAll(response);

    }

    public void deactivate() {
        if (state == State.CONNECTED) {
            setState(State.DISCONNECTED);
        }
        setState(State.NON_CONNECTED);

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray removedActivePeers = new JSONArray();
        JSONObject removedActivePeer = new JSONObject();
        removedActivePeer.put("index", index);
        removedActivePeers.add(removedActivePeer);
        response.put("removedActivePeers", removedActivePeers);

        if (announcedAddress.length() > 0) {
            JSONArray addedKnownPeers = new JSONArray();
            JSONObject addedKnownPeer = new JSONObject();
            addedKnownPeer.put("index", index);
            addedKnownPeer.put("announcedAddress", truncate(announcedAddress, 30, true));
            if (Nxt.wellKnownPeers.contains(announcedAddress)) {
                addedKnownPeer.put("wellKnown", true);
            }
            addedKnownPeers.add(addedKnownPeer);
            response.put("addedKnownPeers", addedKnownPeers);
        }

        User.sendToAll(response);

    }

    public int getWeight() {
        if (accountId == null) {
            return 0;
        }
        Account account = Account.getAccount(accountId);
        if (account == null) {
            return 0;
        }
        return (int)(adjustedWeight * (account.getBalance() / 100) / Nxt.MAX_BALANCE);
    }

    public String getSoftware() {
        StringBuilder buf = new StringBuilder();
        buf.append(truncate(application, 10, false));
        buf.append(" (");
        buf.append(truncate(version, 10, false));
        buf.append(")").append(" @ ");
        buf.append(truncate(platform, 10, false));
        return buf.toString();
    }

    public void removeBlacklistedStatus() {

        setState(State.NON_CONNECTED);
        blacklistingTime = 0;

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray removedBlacklistedPeers = new JSONArray();
        JSONObject removedBlacklistedPeer = new JSONObject();
        removedBlacklistedPeer.put("index", index);
        removedBlacklistedPeers.add(removedBlacklistedPeer);
        response.put("removedBlacklistedPeers", removedBlacklistedPeers);

        JSONArray addedKnownPeers = new JSONArray();
        JSONObject addedKnownPeer = new JSONObject();
        addedKnownPeer.put("index", index);
        addedKnownPeer.put("announcedAddress", truncate(announcedAddress, 30, true));
        if (Nxt.wellKnownPeers.contains(announcedAddress)) {
            addedKnownPeer.put("wellKnown", true);
        }
        addedKnownPeers.add(addedKnownPeer);
        response.put("addedKnownPeers", addedKnownPeers);

        User.sendToAll(response);

    }

    public void removePeer() {

        peers.values().remove(this);

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray removedKnownPeers = new JSONArray();
        JSONObject removedKnownPeer = new JSONObject();
        removedKnownPeer.put("index", index);
        removedKnownPeers.add(removedKnownPeer);
        response.put("removedKnownPeers", removedKnownPeers);

        User.sendToAll(response);

    }

    public JSONObject send(final JSONStreamAware request) {

        JSONObject response;

        String log = null;
        boolean showLog = false;

        HttpURLConnection connection = null;

        try {

            if (Nxt.communicationLoggingMask != 0) {

                StringWriter stringWriter = new StringWriter();
                request.writeJSONString(stringWriter);
                log = "\"" + announcedAddress + "\": " + stringWriter.toString();

            }

            /**/URL url = new URL("http://" + announcedAddress + ((new URL("http://" + announcedAddress)).getPort() < 0 ? ":7874" : "") + "/nxt");
            /**///URL url = new URL("http://" + announcedAddress + ":6874" + "/nxt");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(Nxt.connectTimeout);
            connection.setReadTimeout(Nxt.readTimeout);

            CountingOutputStream cos = new CountingOutputStream(connection.getOutputStream());
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"))) {
                request.writeJSONString(writer);
            }
            updateUploadedVolume(cos.getCount());

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                if ((Nxt.communicationLoggingMask & Nxt.LOGGING_MASK_200_RESPONSES) != 0) {

                    // inefficient
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[65536];
                    int numberOfBytes;
                    try (InputStream inputStream = connection.getInputStream()) {
                        while ((numberOfBytes = inputStream.read(buffer)) > 0) {
                            byteArrayOutputStream.write(buffer, 0, numberOfBytes);
                        }
                    }
                    String responseValue = byteArrayOutputStream.toString("UTF-8");
                    log += " >>> " + responseValue;
                    showLog = true;
                    updateDownloadedVolume(responseValue.getBytes("UTF-8").length);
                    response = (JSONObject) JSONValue.parse(responseValue);

                } else {

                    CountingInputStream cis = new CountingInputStream(connection.getInputStream());

                    try (Reader reader = new BufferedReader(new InputStreamReader(cis, "UTF-8"))) {
                        response = (JSONObject)JSONValue.parse(reader);
                    }

                    updateDownloadedVolume(cis.getCount());

                }

            } else {

                if ((Nxt.communicationLoggingMask & Nxt.LOGGING_MASK_NON200_RESPONSES) != 0) {

                    log += " >>> Peer responded with HTTP " + connection.getResponseCode() + " code!";
                    showLog = true;

                }

                setState(State.DISCONNECTED);

                response = null;

            }

        } catch (RuntimeException|IOException e) {

            if (! (e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof SocketException)) {
                Logger.logDebugMessage("Error sending JSON request", e);
            }

            if ((Nxt.communicationLoggingMask & Nxt.LOGGING_MASK_EXCEPTIONS) != 0) {

                log += " >>> " + e.toString();
                showLog = true;

            }

            if (state == State.NON_CONNECTED) {

                blacklist();

            } else {

                setState(State.DISCONNECTED);

            }

            response = null;

        }

        if (showLog) {

            Logger.logMessage(log + "\n");

        }

        if (connection != null) {

            connection.disconnect();

        }

        return response;

    }

    boolean analyzeHallmark(String realHost, String hallmark) {

        if (hallmark == null || hallmark.equals(this.hallmark)) {
            return true;
        }

        try {
            byte[] hallmarkBytes;
            try {
                hallmarkBytes = Convert.convert(hallmark);
            } catch (NumberFormatException e) {
                return false;
            }
            ByteBuffer buffer = ByteBuffer.wrap(hallmarkBytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte[] publicKey = new byte[32];
            buffer.get(publicKey);
            int hostLength = buffer.getShort();
            if (hostLength > 300) {
                return false;
            }
            byte[] hostBytes = new byte[hostLength];
            buffer.get(hostBytes);
            String host = new String(hostBytes, "UTF-8");
            if (host.length() > 100 || !host.equals(realHost)) {
                return false;
            }
            int weight = buffer.getInt();
            if (weight <= 0 || weight > Nxt.MAX_BALANCE) {
                return false;
            }
            int date = buffer.getInt();
            buffer.get();
            byte[] signature = new byte[64];
            buffer.get(signature);

            byte[] data = new byte[hallmarkBytes.length - 64];
            System.arraycopy(hallmarkBytes, 0, data, 0, data.length);

            if (Crypto.verify(signature, data, publicKey)) {
                this.hallmark = hallmark;
                Long accountId = Account.getId(publicKey);
                LinkedList<Peer> groupedPeers = new LinkedList<>();
                int validDate = 0;
                this.accountId = accountId;
                this.weight = weight;
                this.date = date;
                for (Peer peer : peers.values()) {
                    if (accountId.equals(peer.accountId)) {
                        groupedPeers.add(peer);
                        if (peer.date > validDate) {
                            validDate = peer.date;
                        }
                    }
                }

                long totalWeight = 0;
                for (Peer peer : groupedPeers) {
                    if (peer.date == validDate) {
                        totalWeight += peer.weight;
                    } else {
                        peer.weight = 0;
                    }
                }

                for (Peer peer : groupedPeers) {
                    peer.adjustedWeight = Nxt.MAX_BALANCE * peer.weight / totalWeight;
                    peer.updateWeight();
                }

                return true;
            }
        } catch (RuntimeException|UnsupportedEncodingException e) {
            Logger.logDebugMessage("Failed to analyze hallmark for peer " + realHost, e);
        }
        return false;

    }

    void setState(State state) {

        if (this.state == State.NON_CONNECTED && state != State.NON_CONNECTED) {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            if (announcedAddress.length() > 0) {

                JSONArray removedKnownPeers = new JSONArray();
                JSONObject removedKnownPeer = new JSONObject();
                removedKnownPeer.put("index", index);
                removedKnownPeers.add(removedKnownPeer);
                response.put("removedKnownPeers", removedKnownPeers);

            }

            JSONArray addedActivePeers = new JSONArray();
            JSONObject addedActivePeer = new JSONObject();
            addedActivePeer.put("index", index);
            if (state == State.DISCONNECTED) {
                addedActivePeer.put("disconnected", true);
            }


            addedActivePeer.put("address", truncate(peerAddress, 30, true));
            addedActivePeer.put("announcedAddress", truncate(announcedAddress, 30, true));
            addedActivePeer.put("weight", getWeight());
            addedActivePeer.put("downloaded", downloadedVolume);
            addedActivePeer.put("uploaded", uploadedVolume);
            addedActivePeer.put("software", getSoftware());
            if (Nxt.wellKnownPeers.contains(announcedAddress)) {
                addedActivePeer.put("wellKnown", true);
            }
            addedActivePeers.add(addedActivePeer);
            response.put("addedActivePeers", addedActivePeers);

            User.sendToAll(response);

        } else if (this.state != State.NON_CONNECTED && state != State.NON_CONNECTED) {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            JSONArray changedActivePeers = new JSONArray();
            JSONObject changedActivePeer = new JSONObject();
            changedActivePeer.put("index", index);
            changedActivePeer.put(state == State.CONNECTED ? "connected" : "disconnected", true);
            changedActivePeers.add(changedActivePeer);
            response.put("changedActivePeers", changedActivePeers);

            User.sendToAll(response);

        }

        this.state = state;

    }

    void updateDownloadedVolume(long volume) {

        downloadedVolume += volume;

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray changedActivePeers = new JSONArray();
        JSONObject changedActivePeer = new JSONObject();
        changedActivePeer.put("index", index);
        changedActivePeer.put("downloaded", downloadedVolume);
        changedActivePeers.add(changedActivePeer);
        response.put("changedActivePeers", changedActivePeers);

        User.sendToAll(response);

    }

    void updateUploadedVolume(long volume) {

        uploadedVolume += volume;

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray changedActivePeers = new JSONArray();
        JSONObject changedActivePeer = new JSONObject();
        changedActivePeer.put("index", index);
        changedActivePeer.put("uploaded", uploadedVolume);
        changedActivePeers.add(changedActivePeer);
        response.put("changedActivePeers", changedActivePeers);

        User.sendToAll(response);

    }

    private void connect() {
        JSONObject request = new JSONObject();
        request.put("requestType", "getInfo");
        if (Nxt.myAddress != null && Nxt.myAddress.length() > 0) {
            request.put("announcedAddress", Nxt.myAddress);
        }
        if (Nxt.myHallmark != null && Nxt.myHallmark.length() > 0) {
            request.put("hallmark", Nxt.myHallmark);
        }
        request.put("application", "NRS");
        request.put("version", Nxt.VERSION);
        request.put("platform", Nxt.myPlatform);
        request.put("scheme", Nxt.myScheme);
        request.put("port", Nxt.myPort);
        request.put("shareAddress", Nxt.shareMyAddress);
        JSONObject response = send(JSON.prepareRequest(request));

        if (response != null) {
            application = (String)response.get("application");
            version = (String)response.get("version");
            platform = (String)response.get("platform");
            shareAddress = Boolean.TRUE.equals(response.get("shareAddress"));

            if (analyzeHallmark(announcedAddress, (String)response.get("hallmark"))) {
                setState(State.CONNECTED);
            }
        }
    }

    private void updateWeight() {

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray changedActivePeers = new JSONArray();
        JSONObject changedActivePeer = new JSONObject();
        changedActivePeer.put("index", index);
        changedActivePeer.put("weight", getWeight());
        changedActivePeers.add(changedActivePeer);
        response.put("changedActivePeers", changedActivePeers);

        User.sendToAll(response);

    }

}
