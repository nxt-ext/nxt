package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

class Peer implements Comparable<Peer> {

    static final int STATE_NONCONNECTED = 0;
    static final int STATE_CONNECTED = 1;
    static final int STATE_DISCONNECTED = 2;

    static final Runnable peerConnectingThread = new Runnable() {

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

    };

    static final Runnable peerUnBlacklistingThread = new Runnable() {

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

    };

    static final Runnable getMorePeersThread = new Runnable() {

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

    };

    final int index;
    String platform;
    String announcedAddress;
    boolean shareAddress;
    String hallmark;
    long accountId;
    int weight, date;
    long adjustedWeight;
    String application, version;

    long blacklistingTime;
    int state;
    long downloadedVolume, uploadedVolume;

    Peer(String announcedAddress, int index) {

        this.announcedAddress = announcedAddress;
        this.index = index;

    }

    static Peer addPeer(String address, String announcedAddress) {

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

        if (Nxt.myAddress != null && Nxt.myAddress.length() > 0 && Nxt.myAddress.equals(announcedAddress)) {

            return null;

        }

        Peer peer = Nxt.peers.get(announcedAddress.length() > 0 ? announcedAddress : address);
        if (peer == null) {

            //TODO: Check addresses

            peer = new Peer(announcedAddress, Nxt.peerCounter.incrementAndGet());
            Nxt.peers.put(announcedAddress.length() > 0 ? announcedAddress : address, peer);

        }

        return peer;

    }

    static void updatePeerWeights(Account account) {

        for (Peer peer : Nxt.peers.values()) {

            if (peer.accountId == account.id && peer.adjustedWeight > 0) {

                peer.updateWeight();

            }

        }

    }

    boolean analyzeHallmark(String realHost, String hallmark) {

        if (hallmark == null) {

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

                long accountId = Account.getId(publicKey);
                /*
                Account account = accounts.get(accountId);
                if (account == null) {

                    return false;

                }
                */
                LinkedList<Peer> groupedPeers = new LinkedList<>();
                int validDate = 0;

                this.accountId = accountId;
                this.weight = weight;
                this.date = date;

                for (Peer peer : Nxt.peers.values()) {

                    if (peer.accountId == accountId) {

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

    void blacklist() {

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
        addedBlacklistedPeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
        for (String wellKnownPeer : Nxt.wellKnownPeers) {

            if (announcedAddress.equals(wellKnownPeer)) {

                addedBlacklistedPeer.put("wellKnown", true);

                break;

            }

        }
        addedBlacklistedPeers.add(addedBlacklistedPeer);
        response.put("addedBlacklistedPeers", addedBlacklistedPeers);

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

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
        JSONObject response = send(request);
        if (response != null) {

            application = (String)response.get("application");
            version = (String)response.get("version");
            platform = (String)response.get("platform");
            shareAddress = Boolean.TRUE.equals(response.get("shareAddress"));

            if (analyzeHallmark(announcedAddress, (String)response.get("hallmark"))) {

                setState(STATE_CONNECTED);

            }

        }

    }

    void deactivate() {

        if (state == STATE_CONNECTED) {

            disconnect();

        }
        setState(STATE_NONCONNECTED);

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
            addedKnownPeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
            for (String wellKnownPeer : Nxt.wellKnownPeers) {

                if (announcedAddress.equals(wellKnownPeer)) {

                    addedKnownPeer.put("wellKnown", true);

                    break;

                }

            }
            addedKnownPeers.add(addedKnownPeer);
            response.put("addedKnownPeers", addedKnownPeers);

        }

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

    }

    private void disconnect() {

        setState(STATE_DISCONNECTED);

    }

    static Peer getAnyPeer(int state, boolean applyPullThreshold) {

        List<Peer> selectedPeers = new ArrayList<Peer>();

        for (Peer peer : Nxt.peers.values()) {

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

        for (Peer peer : Nxt.peers.values()) {

            if (peer.state == STATE_CONNECTED && peer.announcedAddress.length() > 0) {

                numberOfConnectedPeers++;

            }

        }

        return numberOfConnectedPeers;

    }

    int getWeight() {

        if (accountId == 0) {

            return 0;

        }
        Account account = Nxt.accounts.get(accountId);
        if (account == null) {

            return 0;

        }

        return (int)(adjustedWeight * (account.getBalance() / 100) / Nxt.MAX_BALANCE);

    }

    String getSoftware() {
        StringBuilder buf = new StringBuilder();
        buf.append(application == null ? "?" : application.substring(0, Math.min(application.length(), 10)));
        buf.append(" (");
        buf.append(version == null ? "?" : version.substring(0, Math.min(version.length(), 10)));
        buf.append(")").append(" @ ");
        buf.append(platform == null ? "?" : platform.substring(0, Math.min(platform.length(), 12)));
        return buf.toString();
    }

    void removeBlacklistedStatus() {

        setState(STATE_NONCONNECTED);
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
        addedKnownPeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
        for (String wellKnownPeer : Nxt.wellKnownPeers) {

            if (announcedAddress.equals(wellKnownPeer)) {

                addedKnownPeer.put("wellKnown", true);

                break;

            }

        }
        addedKnownPeers.add(addedKnownPeer);
        response.put("addedKnownPeers", addedKnownPeers);

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

    }

    void removePeer() {

        Nxt.peers.values().remove(this);

        JSONObject response = new JSONObject();
        response.put("response", "processNewData");

        JSONArray removedKnownPeers = new JSONArray();
        JSONObject removedKnownPeer = new JSONObject();
        removedKnownPeer.put("index", index);
        removedKnownPeers.add(removedKnownPeer);
        response.put("removedKnownPeers", removedKnownPeers);

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

    }

    static void sendToSomePeers(final JSONObject request) {
        request.put("protocol", 1);
        final JSONStreamAware jsonStreamAware = new JSONStreamAware() {
            final char[] jsonChars = request.toJSONString().toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };

        int successful = 0;
        List<Future<JSONObject>> expectedResponses = new ArrayList<>();
        for (final Peer peer : Nxt.peers.values()) {

            if (Nxt.enableHallmarkProtection && peer.getWeight() < Nxt.pushThreshold) {
                continue;
            }

            if (peer.blacklistingTime == 0 && peer.state == Peer.STATE_CONNECTED && peer.announcedAddress.length() > 0) {
                Future<JSONObject> futureResponse = ThreadPools.sendToPeers(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() {
                        return peer.send(jsonStreamAware);
                    }
                });
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

    //TODO: replace usage of this method with send(JSONStreamAware) for requests that are constant
    JSONObject send(final JSONObject request) {
        request.put("protocol", 1);
        return send(new JSONStreamAware() {
            @Override
            public void writeJSONString(Writer out) throws IOException {
                request.writeJSONString(out);
            }
        });
    }

    JSONObject send(final JSONStreamAware request) {

        JSONObject response;

        String log = null;
        boolean showLog = false;

        HttpURLConnection connection = null;

        try {

            if (Nxt.communicationLoggingMask != 0) {

                log = "\"" + announcedAddress + "\": " + request.toString();

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

                disconnect();

                response = null;

            }

        } catch (RuntimeException|IOException e) {

            if (! (e instanceof ConnectException || e instanceof UnknownHostException || e instanceof NoRouteToHostException
                    || e instanceof SocketTimeoutException || e instanceof SocketException)) {
                Logger.logDebugMessage("Error sending JSON request", e);
            }

            if ((Nxt.communicationLoggingMask & Nxt.LOGGING_MASK_EXCEPTIONS) != 0) {

                log += " >>> " + e.toString();
                showLog = true;

            }

            if (state == STATE_NONCONNECTED) {

                blacklist();

            } else {

                disconnect();

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

    void setState(int state) {

        if (this.state == STATE_NONCONNECTED && state != STATE_NONCONNECTED) {

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
            if (state == STATE_DISCONNECTED) {

                addedActivePeer.put("disconnected", true);

            }

            //TODO: there must be a better way
            // cfb: This will be removed after we get a better client
            for (Map.Entry<String, Peer> peerEntry : Nxt.peers.entrySet()) {

                if (peerEntry.getValue() == this) {

                    addedActivePeer.put("address", peerEntry.getKey().length() > 30 ? (peerEntry.getKey().substring(0, 30) + "...") : peerEntry.getKey());

                    break;

                }

            }
            addedActivePeer.put("announcedAddress", announcedAddress.length() > 30 ? (announcedAddress.substring(0, 30) + "...") : announcedAddress);
            addedActivePeer.put("weight", getWeight());
            addedActivePeer.put("downloaded", downloadedVolume);
            addedActivePeer.put("uploaded", uploadedVolume);
            addedActivePeer.put("software", getSoftware());
            for (String wellKnownPeer : Nxt.wellKnownPeers) {

                if (announcedAddress.equals(wellKnownPeer)) {

                    addedActivePeer.put("wellKnown", true);

                    break;

                }

            }
            addedActivePeers.add(addedActivePeer);
            response.put("addedActivePeers", addedActivePeers);

            for (User user : Nxt.users.values()) {

                user.send(response);

            }

        } else if (this.state != STATE_NONCONNECTED && state != STATE_NONCONNECTED) {

            JSONObject response = new JSONObject();
            response.put("response", "processNewData");

            JSONArray changedActivePeers = new JSONArray();
            JSONObject changedActivePeer = new JSONObject();
            changedActivePeer.put("index", index);
            changedActivePeer.put(state == STATE_CONNECTED ? "connected" : "disconnected", true);
            changedActivePeers.add(changedActivePeer);
            response.put("changedActivePeers", changedActivePeers);

            for (User user : Nxt.users.values()) {

                user.send(response);

            }

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

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

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

        for (User user : Nxt.users.values()) {

            user.send(response);

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

        for (User user : Nxt.users.values()) {

            user.send(response);

        }

    }

}
