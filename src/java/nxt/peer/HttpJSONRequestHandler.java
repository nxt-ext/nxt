package nxt.peer;

import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpJSONRequestHandler {

    private static final Map<String,HttpJSONRequestHandler> jsonRequestHandlers;

    static {

        Map<String,HttpJSONRequestHandler> map = new HashMap<>();

        map.put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
        map.put("getInfo", GetInfo.instance);
        map.put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
        map.put("getNextBlockIds", GetNextBlockIds.instance);
        map.put("getNextBlocks", GetNextBlocks.instance);
        map.put("getPeers", GetPeers.instance);
        map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
        map.put("processBlock", ProcessBlock.instance);
        map.put("processTransactions", ProcessTransactions.instance);

        jsonRequestHandlers = Collections.unmodifiableMap(map);
    }

    private static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
    static {
        JSONObject response = new JSONObject();
        response.put("error", "Unsupported request type!");
        UNSUPPORTED_REQUEST_TYPE = JSON.prepare(response);
    }

    private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
    static {
        JSONObject response = new JSONObject();
        response.put("error", "Unsupported protocol!");
        UNSUPPORTED_PROTOCOL = JSON.prepare(response);
    }

    public static void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Peer peer = null;
        JSONStreamAware response = null;

        try {
            JSONObject request;
            CountingInputStream cis = new CountingInputStream(req.getInputStream());
            try (Reader reader = new BufferedReader(new InputStreamReader(cis, "UTF-8"))) {
                request = (JSONObject)JSONValue.parse(reader);
            }
            if (request == null) {
                return;
            }

            peer = Peer.addPeer(req.getRemoteHost(), "");
            if (peer != null) {
                if (peer.state == Peer.State.DISCONNECTED) {
                    peer.setState(Peer.State.CONNECTED);
                }
                peer.updateDownloadedVolume(cis.getCount());
                if (peer.analyzeHallmark(req.getRemoteHost(), (String)request.get("hallmark"))) {
                    peer.setState(Peer.State.CONNECTED);
                }
            }

            if (request.get("protocol") != null && ((Number)request.get("protocol")).intValue() == 1) {
                HttpJSONRequestHandler jsonRequestHandler = jsonRequestHandlers.get((String)request.get("requestType"));
                if (jsonRequestHandler != null) {
                    response = jsonRequestHandler.processJSONRequest(request, peer);
                } else {
                    response = UNSUPPORTED_REQUEST_TYPE;
                }
            } else {
                Logger.logDebugMessage("Unsupported protocol " + request.get("protocol"));
                response = UNSUPPORTED_PROTOCOL;
            }

        } catch (RuntimeException e) {
            Logger.logDebugMessage("Error processing POST request", e);
            JSONObject json = new JSONObject();
            json.put("error", e.toString());
            response = json;
        }

        resp.setContentType("text/plain; charset=UTF-8");
        CountingOutputStream cos = new CountingOutputStream(resp.getOutputStream());
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(cos, "UTF-8"))) {
            response.writeJSONString(writer);
        }

        if (peer != null) {
            peer.updateUploadedVolume(cos.getCount());
        }
    }

    abstract JSONStreamAware processJSONRequest(JSONObject request, Peer peer);

}
