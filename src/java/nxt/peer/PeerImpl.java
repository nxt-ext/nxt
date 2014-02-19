package nxt.peer;

import nxt.Account;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.NxtException;
import nxt.TransactionType;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.Logger;
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
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

final class PeerImpl implements Peer {

    private final String peerAddress;
    private volatile String announcedAddress;
    private volatile int port;
    private volatile boolean shareAddress;
    private volatile Hallmark hallmark;
    private volatile String platform;
    private volatile String application;
    private volatile String version;
    private volatile long adjustedWeight;
    private volatile long blacklistingTime;
    private volatile State state;
    private volatile long downloadedVolume;
    private volatile long uploadedVolume;

    PeerImpl(String peerAddress, String announcedAddress) {
        this.peerAddress = peerAddress;
        this.announcedAddress = announcedAddress;
        try {
            this.port = new URL("http://" + announcedAddress).getPort();
        } catch (MalformedURLException ignore) {}
        this.state = State.NON_CONNECTED;
    }

    @Override
    public String getPeerAddress() {
        return peerAddress;
    }

    @Override
    public State getState() {
        return state;
    }

    void setState(State state) {
        State oldState = this.state;
        this.state = state;
        if (oldState == State.NON_CONNECTED && state != State.NON_CONNECTED) {
            Peers.notifyListeners(this, Peers.Event.ADDED_ACTIVE_PEER);
        } else if (oldState != State.NON_CONNECTED && state != State.NON_CONNECTED) {
            Peers.notifyListeners(this, Peers.Event.CHANGED_ACTIVE_PEER);
        }
    }

    @Override
    public long getDownloadedVolume() {
        return downloadedVolume;
    }

    void updateDownloadedVolume(long volume) {
        downloadedVolume += volume;
        Peers.notifyListeners(this, Peers.Event.DOWNLOADED_VOLUME);
    }

    @Override
    public long getUploadedVolume() {
        return uploadedVolume;
    }

    void updateUploadedVolume(long volume) {
        uploadedVolume += volume;
        Peers.notifyListeners(this, Peers.Event.UPLOADED_VOLUME);
    }

    @Override
    public String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getApplication() {
        return application;
    }

    void setApplication(String application) {
        this.application = application;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String getSoftware() {
        StringBuilder buf = new StringBuilder();
        buf.append(Convert.truncate(application, "?", 10, false));
        buf.append(" (");
        buf.append(Convert.truncate(version, "?", 10, false));
        buf.append(")").append(" @ ");
        buf.append(Convert.truncate(platform, "?", 10, false));
        return buf.toString();
    }

    @Override
    public boolean shareAddress() {
        return shareAddress;
    }

    void setShareAddress(boolean shareAddress) {
        this.shareAddress = shareAddress;
    }

    @Override
    public String getAnnouncedAddress() {
        return announcedAddress;
    }

    void setAnnouncedAddress(String announcedAddress) {
        String announcedPeerAddress = Peers.normalizeHostAndPort(announcedAddress);
        if (announcedPeerAddress != null) {
            this.announcedAddress = announcedPeerAddress;
            try {
                this.port = new URL("http://" + announcedPeerAddress).getPort();
            } catch (MalformedURLException ignore) {}
        }
    }

    @Override
    public boolean isWellKnown() {
        return announcedAddress != null && Peers.wellKnownPeers.contains(announcedAddress);
    }

    @Override
    public Hallmark getHallmark() {
        return hallmark;
    }

    @Override
    public int getWeight() {
        if (hallmark == null) {
            return 0;
        }
        Account account = Account.getAccount(hallmark.getAccountId());
        if (account == null) {
            return 0;
        }
        return (int)(adjustedWeight * (account.getBalance() / 100) / Nxt.MAX_BALANCE);
    }

    @Override
    public boolean isBlacklisted() {
        return blacklistingTime > 0;
    }

    @Override
    public void blacklist(NxtException cause) {
        if (cause instanceof TransactionType.NotYetEnabledException || cause instanceof Blockchain.BlockOutOfOrderException) {
            // don't blacklist peers just because a feature is not yet enabled
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (! isBlacklisted()) {
            Logger.logDebugMessage("Blacklisting " + peerAddress + " because of: " + cause.getMessage());
        }
        blacklist();
    }

    @Override
    public void blacklist() {
        blacklistingTime = System.currentTimeMillis();
        deactivate();
        Peers.notifyListeners(this, Peers.Event.BLACKLIST);
    }

    @Override
    public void unBlacklist() {
        setState(State.NON_CONNECTED);
        blacklistingTime = 0;
        Peers.notifyListeners(this, Peers.Event.UNBLACKLIST);
    }

    void updateBlacklistedStatus(long curTime) {
        if (blacklistingTime > 0 && blacklistingTime + Peers.blacklistingPeriod <= curTime) {
            unBlacklist();
        }
    }

    @Override
    public void deactivate() {
        if (state == State.CONNECTED) {
            setState(State.DISCONNECTED);
        }
        setState(State.NON_CONNECTED);
        Peers.notifyListeners(this, Peers.Event.DEACTIVATE);
    }

    @Override
    public void remove() {
        Peers.removePeer(this);
        Peers.notifyListeners(this, Peers.Event.REMOVE);
    }

    @Override
    public JSONObject send(final JSONStreamAware request) {

        JSONObject response;

        String log = null;
        boolean showLog = false;
        HttpURLConnection connection = null;

        try {

            if (Peers.communicationLoggingMask != 0) {
                StringWriter stringWriter = new StringWriter();
                request.writeJSONString(stringWriter);
                log = "\"" + announcedAddress + "\": " + stringWriter.toString();
            }

            URL url = new URL("http://" + announcedAddress + (port <= 0 ? ":7874" : "") + "/nxt");
            /**///URL url = new URL("http://" + announcedAddress + ":6874" + "/nxt");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(Peers.connectTimeout);
            connection.setReadTimeout(Peers.readTimeout);

            CountingOutputStream cos = new CountingOutputStream(connection.getOutputStream());
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"))) {
                request.writeJSONString(writer);
            }
            updateUploadedVolume(cos.getCount());

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_200_RESPONSES) != 0) {
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

                if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_NON200_RESPONSES) != 0) {
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
            if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
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

    @Override
    public int compareTo(Peer o) {
        if (getWeight() > o.getWeight()) {
            return -1;
        } else if (getWeight() < o.getWeight()) {
            return 1;
        }
        return 0;
    }

    void connect() {
        JSONObject response = send(Peers.myPeerInfoRequest);
        if (response != null) {
            application = (String)response.get("application");
            version = (String)response.get("version");
            platform = (String)response.get("platform");
            shareAddress = Boolean.TRUE.equals(response.get("shareAddress"));
            if (analyzeHallmark(announcedAddress, (String)response.get("hallmark"))) {
                setState(State.CONNECTED);
            } else {
                blacklist();
            }
        }
    }

    boolean analyzeHallmark(String address, final String hallmarkString) {

        if (hallmarkString == null && this.hallmark == null) {
            return true;
        }

        if (this.hallmark != null && this.hallmark.getHallmarkString().equals(hallmarkString)) {
            return true;
        }

        if (hallmarkString == null) {
            this.hallmark = null;
            return true;
        }

        try {
            Hallmark hallmark = Hallmark.parseHallmark(hallmarkString);
            if (! hallmark.isValid() || ! hallmark.getHost().equals(address)) {
                return false;
            }
            this.hallmark = hallmark;
            Long accountId = Account.getId(hallmark.getPublicKey());
            List<PeerImpl> groupedPeers = new ArrayList<>();
            int mostRecentDate = 0;
            long totalWeight = 0;
            for (PeerImpl peer : Peers.allPeers) {
                if (peer.hallmark == null) {
                    continue;
                }
                if (accountId.equals(peer.hallmark.getAccountId())) {
                    groupedPeers.add(peer);
                    if (peer.hallmark.getDate() > mostRecentDate) {
                        mostRecentDate = peer.hallmark.getDate();
                        totalWeight = peer.getHallmarkWeight(mostRecentDate);
                    } else {
                        totalWeight += peer.getHallmarkWeight(mostRecentDate);
                    }
                }
            }

            for (PeerImpl peer : groupedPeers) {
                peer.adjustedWeight = Nxt.MAX_BALANCE * peer.getHallmarkWeight(mostRecentDate) / totalWeight;
                Peers.notifyListeners(peer, Peers.Event.WEIGHT);
            }

            return true;

        } catch (RuntimeException e) {
            Logger.logDebugMessage("Failed to analyze hallmark for peer " + announcedAddress + ", " + e.toString());
        }
        return false;

    }

    private int getHallmarkWeight(int date) {
        if (hallmark == null || ! hallmark.isValid() || hallmark.getDate() != date) {
            return 0;
        }
        return hallmark.getWeight();
    }

}
