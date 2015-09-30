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

package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public final class GetPeers extends APIServlet.APIRequestHandler {

    static final GetPeers instance = new GetPeers();

    private GetPeers() {
        super(new APITag[] {APITag.NETWORK}, "active", "state", "service", "includePeerInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        boolean active = "true".equalsIgnoreCase(req.getParameter("active"));
        String stateValue = Convert.emptyToNull(req.getParameter("state"));
        String serviceValue = Convert.emptyToNull(req.getParameter("service"));
        boolean includePeerInfo = "true".equalsIgnoreCase(req.getParameter("includePeerInfo"));
        Peer.State state;
        if (stateValue != null) {
            try {
                state = Peer.State.valueOf(stateValue);
            } catch (RuntimeException exc) {
                return JSONResponses.incorrect("state", "- '" + stateValue + "' is not defined");
            }
        } else {
            state = null;
        }
        Peer.Service service;
        if (serviceValue != null) {
            try {
                service = Peer.Service.valueOf(serviceValue);
            } catch (RuntimeException exc) {
                return JSONResponses.incorrect("service", "- '" + serviceValue + "' is not defined");
            }
        } else {
            service = null;
        }

        Collection<? extends Peer> peers = active ? Peers.getActivePeers() : state != null ? Peers.getPeers(state) : Peers.getAllPeers();
        JSONArray peersJSON = new JSONArray();
        if (service != null) {
            if (includePeerInfo) {
                peers.forEach(peer -> {
                    if (peer.providesService(service)) {
                        peersJSON.add(JSONData.peer(peer));
                    }
                });
            } else {
                peers.forEach(peer -> {
                    if (peer.providesService(service)) {
                        peersJSON.add(peer.getHost());
                    }
                });
            }
        } else {
            if (includePeerInfo) {
                peers.forEach(peer -> peersJSON.add(JSONData.peer(peer)));
            } else {
                peers.forEach(peer -> peersJSON.add(peer.getHost()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("peers", peersJSON);
        return response;
    }

    @Override
    boolean allowRequiredBlockParameters() {
        return false;
    }

}
