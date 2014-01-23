package nxt.http;

import nxt.util.JSON;
import nxt.Nxt;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpRequestHandler {

    private static final Map<String,HttpRequestHandler> httpGetHandlers;

    static {

        Map<String,HttpRequestHandler> map = new HashMap<>();

        map.put("assignAlias", AssignAlias.instance);
        map.put("broadcastTransaction", BroadcastTransaction.instance);
        map.put("decodeHallmark", DecodeHallmark.instance);
        map.put("decodeToken", DecodeToken.instance);
        map.put("getAccountBlockIds", GetAccountBlockIds.instance);
        map.put("getAccountId", GetAccountId.instance);
        map.put("getAccountPublicKey", GetAccountPublicKey.instance);
        map.put("getAccountTransactionIds", GetAccountTransactionIds.instance);
        map.put("getAlias", GetAlias.instance);
        map.put("getAliasId", GetAliasId.instance);
        map.put("getAliasIds", GetAliasIds.instance);
        map.put("getAliasURI", GetAliasURI.instance);
        map.put("getBalance", GetBalance.instance);
        map.put("getBlock", GetBlock.instance);
        map.put("getConstants", GetConstants.instance);
        map.put("getGuaranteedBalance", GetGuaranteedBalance.instance);
        map.put("getMyInfo", GetMyInfo.instance);
        map.put("getPeer", GetPeer.instance);
        map.put("getPeers", GetPeers.instance);
        map.put("getState", GetState.instance);
        map.put("getTime", GetTime.instance);
        map.put("getTransaction", GetTransaction.instance);
        map.put("getTransactionBytes", GetTransactionBytes.instance);
        map.put("getUnconfirmedTransactionIds", GetUnconfirmedTransactionIds.instance);
        map.put("getAccountCurrentAskOrderIds", GetAccountCurrentAskOrderIds.instance);
        map.put("getAccountCurrentBidOrderIds", GetAccountCurrentBidOrderIds.instance);
        map.put("getAskOrder", GetAskOrder.instance);
        map.put("getAskOrderIds", GetAskOrderIds.instance);
        map.put("getBidOrder", GetBidOrder.instance);
        map.put("getBidOrderIds", GetBidOrderIds.instance);
        map.put("listAccountAliases", ListAccountAliases.instance);
        map.put("markHost", MarkHost.instance);
        map.put("sendMessage", SendMessage.instance);
        map.put("sendMoney", SendMoney.instance);

        //TODO: those are still disabled
        //map.put("issueAsset", IssueAsset.instance);
        //map.put("cancelAskOrder", CancelAskOrder.instance);
        //map.put("cancelBidOrder", CancelBidOrder.instance);
        //map.put("getAsset", GetAsset.instance);
        //map.put("getAssetIds", GetAssetIds.instance);
        //map.put("transferAsset", TransferAsset.instance);
        //map.put("placeAskOrder", PlaceAskOrder.instance);
        //map.put("placeBidOrder", PlaceBidOrder.instance);

        httpGetHandlers = Collections.unmodifiableMap(map);
    }

    private static final JSONStreamAware ERROR_NOT_ALLOWED;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 7);
        response.put("errorDescription", "Not allowed");
        ERROR_NOT_ALLOWED = JSON.getJSONStreamAware(response);
    }

    private static final JSONStreamAware ERROR_INCORRECT_REQUEST;
    static {
        JSONObject response  = new JSONObject();
        response.put("errorCode", 1);
        response.put("errorDescription", "Incorrect request");
        ERROR_INCORRECT_REQUEST = JSON.getJSONStreamAware(response);
    }

    public static void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        JSONStreamAware response;

        if (Nxt.allowedBotHosts != null && !Nxt.allowedBotHosts.contains(req.getRemoteHost())) {
            response = ERROR_NOT_ALLOWED;
        } else {

            String requestType = req.getParameter("requestType");
            if (requestType == null) {
                response = ERROR_INCORRECT_REQUEST;
            } else {

                HttpRequestHandler requestHandler = httpGetHandlers.get(requestType);
                if (requestHandler != null) {
                    response = requestHandler.processRequest(req);
                } else {
                    response = ERROR_INCORRECT_REQUEST;
                }

            }

        }

        resp.setContentType("text/plain; charset=UTF-8");

        try (Writer writer = resp.getWriter()) {
            response.writeJSONString(writer);
        }

    }

    abstract JSONStreamAware processRequest(HttpServletRequest request) throws IOException;

}
