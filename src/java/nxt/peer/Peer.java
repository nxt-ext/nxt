package nxt.peer;

import nxt.Account;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.NxtException;
import nxt.ThreadPools;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
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
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
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

    public static enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER
    }

    private static final Listeners<Peer,Event> listeners = new Listeners<>();
    private static final AtomicInteger peerCounter = new AtomicInteger();
    private static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();
    private static final Collection<Peer> allPeers = Collections.unmodifiableCollection(peers.values());

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                for (Peer peer : peers.values()) {
                    if (account.getId().equals(peer.accountId) && peer.adjustedWeight > 0) {
                        Peer.listeners.notify(peer, Event.WEIGHT);
                    }
                }
            }
        }, Account.Event.BALANCE);
    }

    public static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    if (Peer.getNumberOfConnectedPublicPeers() < Nxt.maxNumberOfConnectedPublicPeers) {
                        Peer peer = Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? State.NON_CONNECTED : State.DISCONNECTED, false);
                        if (peer != null) {
                            peer.connect();
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error connecting to peer", e);
                }
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
                try {

                    long curTime = System.currentTimeMillis();
                    for (Peer peer : peers.values()) {
                        if (peer.blacklistingTime > 0 && peer.blacklistingTime + Nxt.blacklistingPeriod <= curTime ) {
                            peer.removeBlacklistedStatus();
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error un-blacklisting peer", e);
                }
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
                try {

                    Peer peer = Peer.getAnyPeer(State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getPeersRequest);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    for (Object peerAddress : peers) {
                        String address = ((String)peerAddress).trim();
                        if (address.length() > 0) {
                            Peer.addPeer(address, address);
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Collection<Peer> getAllPeers() {
        return allPeers;
    }

    public static Peer getPeer(String peerAddress) {
        return peers.get(peerAddress);
    }

    public static Peer addPeer(final String address, final String announcedAddress) {

        String peerAddress = parseHostAndPort(address);
        if (peerAddress == null) {
            return null;
        }

        String announcedPeerAddress = parseHostAndPort(announcedAddress);

        if (Nxt.myAddress != null && Nxt.myAddress.length() > 0 && Nxt.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
            return null;
        }

        if (announcedPeerAddress != null) {
            peerAddress = announcedPeerAddress;
        }

        Peer peer = peers.get(peerAddress);
        if (peer == null) {
            peer = new Peer(peerAddress, announcedPeerAddress);
            peers.put(peerAddress, peer);
        }

        return peer;
    }

    public static void sendToSomePeers(final JSONObject request) {

        final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

        int successful = 0;
        List<Future<JSONObject>> expectedResponses = new ArrayList<>();
        for (final Peer peer : peers.values()) {

            if (Nxt.enableHallmarkProtection && peer.getWeight() < Nxt.pushThreshold) {
                continue;
            }

            if (! peer.isBlacklisted() && peer.state == State.CONNECTED && peer.announcedAddress != null) {
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

        List<Peer> selectedPeers = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (! peer.isBlacklisted() && peer.state == state && peer.announcedAddress != null
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

    private static String parseHostAndPort(String address) {
        try {
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();
            if (host == null || host.equals("") || host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0:0:0:0:0:0:0:1")) {
                return null;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
                return null;
            }
            int port = uri.getPort();
            return port == -1 ? host : host + ':' + port;
        } catch (URISyntaxException|UnknownHostException e) {
            return null;
        }
    }

    private static int getNumberOfConnectedPublicPeers() {
        int numberOfConnectedPeers = 0;
        for (Peer peer : peers.values()) {
            if (peer.state == State.CONNECTED && peer.announcedAddress != null) {
                numberOfConnectedPeers++;
            }
        }
        return numberOfConnectedPeers;
    }


    private final int index;
    private final String peerAddress;
    private String announcedAddress;
    private int port;
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
        try {
            this.port = new URL("http://" + announcedAddress).getPort();
        } catch (MalformedURLException ignore) {}
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
        String announcedPeerAddress = parseHostAndPort(announcedAddress);
        if (announcedPeerAddress != null) {
            this.announcedAddress = announcedPeerAddress;
            try {
                this.port = new URL("http://" + announcedPeerAddress).getPort();
            } catch (MalformedURLException ignore) {}
        }
    }

    public boolean isWellKnown() {
        return announcedAddress != null && Nxt.wellKnownPeers.contains(announcedAddress);
    }

    public boolean isBlacklisted() {
        return blacklistingTime > 0;
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

    public void blacklist(NxtException cause) {
        if (cause instanceof Transaction.NotYetEnabledException || cause instanceof Blockchain.BlockOutOfOrderException) {
            // don't blacklist peers just because a feature is not yet enabled
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (! isBlacklisted()) {
            Logger.logDebugMessage("Blacklisting " + peerAddress + " because of: " + cause.getMessage());
        }
        blacklist();
    }

    public void blacklist() {
        blacklistingTime = System.currentTimeMillis();
        deactivate();
        listeners.notify(this, Event.BLACKLIST);
    }

    public void deactivate() {
        if (state == State.CONNECTED) {
            setState(State.DISCONNECTED);
        }
        setState(State.NON_CONNECTED);
        listeners.notify(this, Event.DEACTIVATE);
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
        buf.append(Convert.truncate(application, "?", 10, false));
        buf.append(" (");
        buf.append(Convert.truncate(version, "?", 10, false));
        buf.append(")").append(" @ ");
        buf.append(Convert.truncate(platform, "?", 10, false));
        return buf.toString();
    }

    public void removeBlacklistedStatus() {
        setState(State.NON_CONNECTED);
        blacklistingTime = 0;
        listeners.notify(this, Event.UNBLACKLIST);
    }

    public void removePeer() {
        peers.values().remove(this);
        listeners.notify(this, Event.REMOVE);
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

            URL url = new URL("http://" + announcedAddress + (port <= 0 ? ":7874" : "") + "/nxt");
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

    void setState(State state) {
        State oldState = this.state;
        this.state = state;
        if (oldState == State.NON_CONNECTED && state != State.NON_CONNECTED) {
            listeners.notify(this, Event.ADDED_ACTIVE_PEER);
        } else if (oldState != State.NON_CONNECTED && state != State.NON_CONNECTED) {
            listeners.notify(this, Event.CHANGED_ACTIVE_PEER);
        }
    }

    void updateDownloadedVolume(long volume) {
        downloadedVolume += volume;
        listeners.notify(this, Event.DOWNLOADED_VOLUME);
    }

    void updateUploadedVolume(long volume) {
        uploadedVolume += volume;
        listeners.notify(this, Event.UPLOADED_VOLUME);
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

    private boolean analyzeHallmark(final String realHost, final String hallmarkString) {

        if (hallmarkString == null || hallmarkString.equals(this.hallmark)) {
            return true;
        }

        try {
            Hallmark hallmark = Hallmark.parseHallmark(hallmarkString);
            if (! hallmark.isValid() || ! hallmark.getHost().equals(realHost)) {
                return false;
            }
            this.hallmark = hallmarkString;
            Long accountId = Account.getId(hallmark.getPublicKey());
            LinkedList<Peer> groupedPeers = new LinkedList<>();
            int validDate = 0;
            this.accountId = accountId;
            this.weight = hallmark.getWeight();
            this.date = hallmark.getDate();
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
                listeners.notify(peer, Event.WEIGHT);
            }

            return true;

        } catch (RuntimeException e) {
            Logger.logDebugMessage("Failed to analyze hallmark for peer " + realHost + ", " + e.toString());
        }
        return false;

    }

}
