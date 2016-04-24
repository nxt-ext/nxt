/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import nxt.Constants;
import nxt.Nxt;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class APIProxy {
    private static APIProxy instance = new APIProxy();

    public static final boolean enableAPIProxy = Nxt.getBooleanProperty("nxt.enableAPIProxy");
    public static final int blacklistingPeriod = Nxt.getIntProperty("nxt.blacklistingPeriod") / 1000;

    private String currentPeerHost;

    private Random randomGenerator;

    private ConcurrentHashMap<String, Integer> blacklistedPeers = new ConcurrentHashMap<>();

    private static final Runnable peerUnBlacklistingThread = () -> {
        int curTime = Nxt.getEpochTime();
        instance.blacklistedPeers.entrySet().removeIf((entry) -> {
            if (entry.getValue() < curTime) {
                Logger.logDebugMessage("Unblacklisting API peer " + entry.getKey());
                return true;
            }
            return false;
        });
    };

    static{
        if (!Constants.isOffline) {
            ThreadPool.scheduleThread("APIProxyPeerUnBlacklisting", peerUnBlacklistingThread, blacklistingPeriod);
        }
    }

    private APIProxy() {
        randomGenerator = new Random();
    }

    public static void init() {}

    public static APIProxy getInstance() {
        return instance;
    }

    public Peer getServingPeer() {
        Peer peer = null;

        String currentPeerHost = this.currentPeerHost;
        if (currentPeerHost != null) {
            peer = Peers.getPeer(currentPeerHost);
        }

        if (peer == null
                || peer.getState() != Peer.State.CONNECTED
                || !isOpenAPIPeer(peer)) {
            peer = getRandomAPIPeer();
            if (peer != null) {
                this.currentPeerHost = peer.getHost();
            }
        }

        return peer;
    }

    public boolean setServingPeer(Peer peer) {
        if (peer != null && peer.getState() == Peer.State.CONNECTED && isOpenAPIPeer(peer)) {
            currentPeerHost = peer.getHost();
            return true;
        }
        return false;
    }

    public static boolean isActivated() {
        return enableAPIProxy && (Nxt.getBlockchainProcessor().isDownloading() || Constants.isLightClient || Constants.isOffline);
    }

    public void blacklistHost(String host) {
        if (host.equals(currentPeerHost)) {
            currentPeerHost = null;
        }
        blacklistedPeers.put(host, Nxt.getEpochTime() + blacklistingPeriod);
    }

    private Peer getRandomAPIPeer() {
        List<Peer> peers = Peers.getPeers(peer -> !blacklistedPeers.contains(peer.getHost())
                && peer.getState() == Peer.State.CONNECTED && isOpenAPIPeer(peer));

        if (peers.isEmpty()) {
            return null;
        }
        int index = randomGenerator.nextInt(peers.size());
        return peers.get(index);
    }

    public static boolean isOpenAPIPeer(Peer peer) {
        return peer.providesService(Peer.Service.API) || peer.providesService(Peer.Service.API_SSL);
        //return peer.providesService(Peer.Service.API);
    }
}
