package nxt.http;

import nxt.Db;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nxt.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static nxt.http.JSONResponses.ERROR_NOT_ALLOWED;
import static nxt.http.JSONResponses.INCORRECT_ADMIN_PASSWORD;
import static nxt.http.JSONResponses.NO_PASSWORD_IN_CONFIG;
import static nxt.http.JSONResponses.POST_REQUIRED;

public final class APIServlet extends HttpServlet {

    abstract static class APIRequestHandler {

        private final List<String> parameters;
        private final Set<APITag> apiTags;

        APIRequestHandler(APITag[] apiTags, String... parameters) {
            List<String> parametersList;
            if (requirePassword() && ! API.disableAdminPassword) {
                parametersList = new ArrayList<>(parameters.length + 1);
                parametersList.add("adminPassword");
                parametersList.addAll(Arrays.asList(parameters));
            } else {
                parametersList = Arrays.asList(parameters);
            }
            this.parameters = Collections.unmodifiableList(parametersList);
            this.apiTags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(apiTags)));
        }

        final List<String> getParameters() {
            return parameters;
        }

        final Set<APITag> getAPITags() {
            return apiTags;
        }

        abstract JSONStreamAware processRequest(HttpServletRequest request) throws NxtException;

        boolean requirePost() {
            return false;
        }

        boolean startDbTransaction() {
            return false;
        }
        
        boolean requirePassword() {
        	return false;
        }
    }

    private static final boolean enforcePost = Nxt.getBooleanProperty("nxt.apiServerEnforcePOST");
    static final Map<String,APIRequestHandler> apiRequestHandlers;

    static {

        Map<String,APIRequestHandler> map = new HashMap<>();

        map.put("broadcastTransaction", BroadcastTransaction.instance);
        map.put("calculateFullHash", CalculateFullHash.instance);
        map.put("cancelAskOrder", CancelAskOrder.instance);
        map.put("cancelBidOrder", CancelBidOrder.instance);
        map.put("currencyBuy", CurrencyBuy.instance);
        map.put("currencySell", CurrencySell.instance);
        map.put("currencyReserveIncrease", CurrencyReserveIncrease.instance);
        map.put("currencyReserveClaim", CurrencyReserveClaim.instance);
        map.put("currencyMint", CurrencyMint.instance);
        //map.put("castVote", CastVote.instance);
        //map.put("createPoll", CreatePoll.instance);
        map.put("decryptFrom", DecryptFrom.instance);
        map.put("dgsListing", DGSListing.instance);
        map.put("dgsDelisting", DGSDelisting.instance);
        map.put("dgsDelivery", DGSDelivery.instance);
        map.put("dgsFeedback", DGSFeedback.instance);
        map.put("dgsPriceChange", DGSPriceChange.instance);
        map.put("dgsPurchase", DGSPurchase.instance);
        map.put("dgsQuantityChange", DGSQuantityChange.instance);
        map.put("dgsRefund", DGSRefund.instance);
        map.put("decodeHallmark", DecodeHallmark.instance);
        map.put("decodeToken", DecodeToken.instance);
        map.put("encryptTo", EncryptTo.instance);
        map.put("generateToken", GenerateToken.instance);
        map.put("getAccount", GetAccount.instance);
        map.put("getAccountBlockCount", GetAccountBlockCount.instance);
        map.put("getAccountBlockIds", GetAccountBlockIds.instance);
        map.put("getAccountBlocks", GetAccountBlocks.instance);
        map.put("getAccountId", GetAccountId.instance);
        map.put("getAccountPublicKey", GetAccountPublicKey.instance);
        map.put("getAccountTransactionIds", GetAccountTransactionIds.instance);
        map.put("getAccountTransactions", GetAccountTransactions.instance);
        map.put("getAccountLessors", GetAccountLessors.instance);
        map.put("getAccountAssets", GetAccountAssets.instance);
        map.put("getAccountCurrencies", GetAccountCurrencies.instance);
        map.put("getAccountCurrencyCount", GetAccountCurrencyCount.instance);
        map.put("getAccountAssetCount", GetAccountAssetCount.instance);
        map.put("sellAlias", SellAlias.instance);
        map.put("buyAlias", BuyAlias.instance);
        map.put("getAlias", GetAlias.instance);
        map.put("getAliasCount", GetAliasCount.instance);
        map.put("getAliases", GetAliases.instance);
        map.put("getAllAssets", GetAllAssets.instance);
        map.put("getAllCurrencies", GetAllCurrencies.instance);
        map.put("getAsset", GetAsset.instance);
        map.put("getAssets", GetAssets.instance);
        map.put("getAssetIds", GetAssetIds.instance);
        map.put("getAssetsByIssuer", GetAssetsByIssuer.instance);
        map.put("getAssetAccounts", GetAssetAccounts.instance);
        map.put("getAssetAccountCount", GetAssetAccountCount.instance);
        map.put("getBalance", GetBalance.instance);
        map.put("getBlock", GetBlock.instance);
        map.put("getBlockId", GetBlockId.instance);
        map.put("getBlocks", GetBlocks.instance);
        map.put("getBlockchainStatus", GetBlockchainStatus.instance);
        map.put("getConstants", GetConstants.instance);
        map.put("getCurrency", GetCurrency.instance);
        map.put("getCurrencies", GetCurrencies.instance);
        map.put("getCurrencyFounders", GetCurrencyFounders.instance);
        map.put("getCurrencyIds", GetCurrencyIds.instance);
        map.put("getCurrenciesByIssuer", GetCurrenciesByIssuer.instance);
        map.put("getCurrencyAccounts", GetCurrencyAccounts.instance);
        map.put("getCurrencyAccountCount", GetCurrencyAccountCount.instance);
        map.put("getDGSGoods", GetDGSGoods.instance);
        map.put("getDGSGoodsCount", GetDGSGoodsCount.instance);
        map.put("getDGSGood", GetDGSGood.instance);
        map.put("getDGSGoodsPurchases", GetDGSGoodsPurchases.instance);
        map.put("getDGSGoodsPurchaseCount", GetDGSGoodsPurchaseCount.instance);
        map.put("getDGSPurchases", GetDGSPurchases.instance);
        map.put("getDGSPurchase", GetDGSPurchase.instance);
        map.put("getDGSPurchaseCount", GetDGSPurchaseCount.instance);
        map.put("getDGSPendingPurchases", GetDGSPendingPurchases.instance);
        map.put("getDGSTags", GetDGSTags.instance);
        map.put("getDGSTagCount", GetDGSTagCount.instance);
        map.put("getGuaranteedBalance", GetGuaranteedBalance.instance);
        map.put("getECBlock", GetECBlock.instance);
        map.put("getMyInfo", GetMyInfo.instance);
        //map.put("getNextBlockGenerators", GetNextBlockGenerators.instance);
        map.put("getPeer", GetPeer.instance);
        map.put("getPeers", GetPeers.instance);
        //map.put("getPoll", GetPoll.instance);
        //map.put("getPollIds", GetPollIds.instance);
        map.put("getState", GetState.instance);
        map.put("getTime", GetTime.instance);
        map.put("getTrades", GetTrades.instance);
        map.put("getExchanges", GetExchanges.instance);
        map.put("getExchangesByExchangeRequest", GetExchangesByExchangeRequest.instance);
        map.put("getExchangesByOffer", GetExchangesByOffer.instance);
        map.put("getAllTrades", GetAllTrades.instance);
        map.put("getAllExchanges", GetAllExchanges.instance);
        map.put("getAssetTransfers", GetAssetTransfers.instance);
        map.put("getCurrencyTransfers", GetCurrencyTransfers.instance);
        map.put("getTransaction", GetTransaction.instance);
        map.put("getTransactionBytes", GetTransactionBytes.instance);
        map.put("getUnconfirmedTransactionIds", GetUnconfirmedTransactionIds.instance);
        map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
        map.put("getAccountCurrentAskOrderIds", GetAccountCurrentAskOrderIds.instance);
        map.put("getAccountCurrentBidOrderIds", GetAccountCurrentBidOrderIds.instance);
        map.put("getAccountCurrentAskOrders", GetAccountCurrentAskOrders.instance);
        map.put("getAccountCurrentBidOrders", GetAccountCurrentBidOrders.instance);
        map.put("getAllOpenAskOrders", GetAllOpenAskOrders.instance);
        map.put("getAllOpenBidOrders", GetAllOpenBidOrders.instance);
        map.put("getBuyOffers", GetBuyOffers.instance);
        map.put("getSellOffers", GetSellOffers.instance);
        map.put("getOffer", GetOffer.instance);
        map.put("getAskOrder", GetAskOrder.instance);
        map.put("getAskOrderIds", GetAskOrderIds.instance);
        map.put("getAskOrders", GetAskOrders.instance);
        map.put("getBidOrder", GetBidOrder.instance);
        map.put("getBidOrderIds", GetBidOrderIds.instance);
        map.put("getBidOrders", GetBidOrders.instance);
        map.put("getAccountExchangeRequests", GetAccountExchangeRequests.instance);
        map.put("getMintingTarget", GetMintingTarget.instance);
        map.put("issueAsset", IssueAsset.instance);
        map.put("issueCurrency", IssueCurrency.instance);
        map.put("leaseBalance", LeaseBalance.instance);
        map.put("longConvert", LongConvert.instance);
        map.put("markHost", MarkHost.instance);
        map.put("parseTransaction", ParseTransaction.instance);
        map.put("placeAskOrder", PlaceAskOrder.instance);
        map.put("placeBidOrder", PlaceBidOrder.instance);
        map.put("publishExchangeOffer", PublishExchangeOffer.instance);
        map.put("rsConvert", RSConvert.instance);
        map.put("readMessage", ReadMessage.instance);
        map.put("sendMessage", SendMessage.instance);
        map.put("sendMoney", SendMoney.instance);
        map.put("setAccountInfo", SetAccountInfo.instance);
        map.put("setAlias", SetAlias.instance);
        map.put("deleteAlias", DeleteAlias.instance);
        map.put("signTransaction", SignTransaction.instance);
        map.put("startForging", StartForging.instance);
        map.put("stopForging", StopForging.instance);
        map.put("getForging", GetForging.instance);
        map.put("transferAsset", TransferAsset.instance);
        map.put("transferCurrency", TransferCurrency.instance);
        map.put("canDeleteCurrency", CanDeleteCurrency.instance);
        map.put("deleteCurrency", DeleteCurrency.instance);
        map.put("dividendPayment", DividendPayment.instance);
        map.put("searchDGSGoods", SearchDGSGoods.instance);
        map.put("searchAssets", SearchAssets.instance);
        map.put("searchCurrencies", SearchCurrencies.instance);
        map.put("clearUnconfirmedTransactions", ClearUnconfirmedTransactions.instance);
        map.put("fullReset", FullReset.instance);
        map.put("popOff", PopOff.instance);
        map.put("scan", Scan.instance);
        map.put("luceneReindex", LuceneReindex.instance);
        map.put("addPeer", AddPeer.instance);
        map.put("blacklistPeer", BlacklistPeer.instance);
        
        apiRequestHandlers = Collections.unmodifiableMap(map);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        JSONStreamAware response = JSON.emptyJSON;

        try {

            long startTime = System.currentTimeMillis();

			if (! API.isAllowed(req.getRemoteHost())) {
                response = ERROR_NOT_ALLOWED;
                return;
            }

            String requestType = req.getParameter("requestType");
            if (requestType == null) {
                response = ERROR_INCORRECT_REQUEST;
                return;
            }

            APIRequestHandler apiRequestHandler = apiRequestHandlers.get(requestType);
            if (apiRequestHandler == null) {
                response = ERROR_INCORRECT_REQUEST;
                return;
            }

            if (enforcePost && apiRequestHandler.requirePost() && ! "POST".equals(req.getMethod())) {
                response = POST_REQUIRED;
                return;
            }

            if (apiRequestHandler.requirePassword() && !API.disableAdminPassword) {
                if (API.adminPassword.isEmpty()) {
                    response = NO_PASSWORD_IN_CONFIG;
                    return;
                } else if (!API.adminPassword.equals(req.getParameter("adminPassword"))) {
                    response = INCORRECT_ADMIN_PASSWORD;
                    return;
                }
            }

            try {
                if (apiRequestHandler.startDbTransaction()) {
                    Db.db.beginTransaction();
                }
                response = apiRequestHandler.processRequest(req);
            } catch (ParameterException e) {
                response = e.getErrorResponse();
            } catch (NxtException |RuntimeException e) {
                Logger.logDebugMessage("Error processing API request", e);
                response = ERROR_INCORRECT_REQUEST;
            } catch (ExceptionInInitializerError err) {
                Logger.logErrorMessage("Initialization Error", (Exception) err.getCause());
                response = ERROR_INCORRECT_REQUEST;
            } finally {
                if (apiRequestHandler.startDbTransaction()) {
                    Db.db.endTransaction();
                }
            }

            if (response instanceof JSONObject) {
                ((JSONObject)response).put("requestProcessingTime", System.currentTimeMillis() - startTime);
            }

        } finally {
            resp.setContentType("text/plain; charset=UTF-8");
            try (Writer writer = resp.getWriter()) {
                response.writeJSONString(writer);
            }
        }

    }

}
