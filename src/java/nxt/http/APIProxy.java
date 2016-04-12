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

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class APIProxy {
    private static APIProxy instance = new APIProxy();

    public static final boolean enableAPIProxy = Nxt.getBooleanProperty("nxt.enableAPIProxy");

    private String currentPeerHost;

    private Random randomGenerator;

    private ConcurrentHashMap<String, Integer> blacklistedPeers = new ConcurrentHashMap<>();

    private APIProxy() {
        randomGenerator = new Random();
    }

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

    public static boolean isActivated() {
        return enableAPIProxy && (Nxt.getBlockchainProcessor().isDownloading() || Constants.isOffline);
    }

    public void blacklistHost(String host) {
        if (host.equals(currentPeerHost)) {
            currentPeerHost = null;
        }
        blacklistedPeers.put(host, Nxt.getEpochTime() + 10*60);
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

    private boolean isOpenAPIPeer(Peer peer) {
        return peer.providesService(Peer.Service.API) || peer.providesService(Peer.Service.API_SSL);
        //return peer.providesService(Peer.Service.API);
    }
}
