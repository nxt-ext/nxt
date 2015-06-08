/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
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

package nxt.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.stream.Collectors;

final class GetPeers extends PeerServlet.PeerRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        response.put("peers", Peers.getAllPeers().parallelStream().unordered()
                .filter(otherPeer -> ! otherPeer.isBlacklisted() && otherPeer.getAnnouncedAddress() != null
                        && otherPeer.getState() == Peer.State.CONNECTED && otherPeer.shareAddress())
                .map(Peer::getAnnouncedAddress)
                .collect(Collectors.toCollection(JSONArray::new)));

        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return false;
    }

}
