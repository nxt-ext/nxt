package nxt.http;

import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpRequestHandler {

    private static final Map<String,HttpRequestHandler> handlers;

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

        handlers = Collections.unmodifiableMap(map);
    }

    public static HttpRequestHandler getHandler(String requestType) {
        return handlers.get(requestType);
    }

    public abstract JSONObject processRequest(HttpServletRequest request);

}
