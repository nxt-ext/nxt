package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

public final class GetAccount extends APIServlet.APIRequestHandler {

    static final GetAccount instance = new GetAccount();

    private GetAccount() {
        super(new APITag[] {APITag.ACCOUNTS}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);

        synchronized (account) {
            JSONObject response = JSONData.accountBalance(account);
            response.put("account", Convert.toUnsignedLong(account.getId()));
            response.put("accountRS", Convert.rsAccount(account.getId()));

            if (account.getPublicKey() != null) {
                response.put("publicKey", Convert.toHexString(account.getPublicKey()));
            }
            if (account.getName() != null) {
                response.put("name", account.getName());
            }
            if (account.getDescription() != null) {
                response.put("description", account.getDescription());
            }
            if (account.getCurrentLesseeId() != null) {
                response.put("currentLessee", Convert.toUnsignedLong(account.getCurrentLesseeId()));
                response.put("currentLesseeRS", Convert.rsAccount(account.getCurrentLesseeId()));
                response.put("currentLeasingHeightFrom", account.getCurrentLeasingHeightFrom());
                response.put("currentLeasingHeightTo", account.getCurrentLeasingHeightTo());
                if (account.getNextLesseeId() != null) {
                    response.put("nextLessee", Convert.toUnsignedLong(account.getNextLesseeId()));
                    response.put("nextLesseeRS", Convert.rsAccount(account.getNextLesseeId()));
                    response.put("nextLeasingHeightFrom", account.getNextLeasingHeightFrom());
                    response.put("nextLeasingHeightTo", account.getNextLeasingHeightTo());
                }
            }
            Set<Long> lessors = account.getLessorIds();
            if (!lessors.isEmpty()) {

                JSONArray lessorIds = new JSONArray();
                JSONArray lessorIdsRS = new JSONArray();
                for (Long lessorId : lessors) {
                    lessorIds.add(Convert.toUnsignedLong(lessorId));
                    lessorIdsRS.add(Convert.rsAccount(lessorId));
                }
                response.put("lessors", lessorIds);
                response.put("lessorsRS", lessorIdsRS);
            }

            JSONArray assetBalances = new JSONArray();
            for (Map.Entry<Long, Long> assetBalanceEntry : account.getAssetBalancesQNT().entrySet()) {

                JSONObject assetBalance = new JSONObject();
                assetBalance.put("asset", Convert.toUnsignedLong(assetBalanceEntry.getKey()));
                assetBalance.put("balanceQNT", String.valueOf(assetBalanceEntry.getValue()));
                assetBalances.add(assetBalance);

            }
            if (assetBalances.size() > 0) {
                response.put("assetBalances", assetBalances);
            }

            JSONArray unconfirmedAssetBalances = new JSONArray();
            for (Map.Entry<Long, Long> unconfirmedAssetBalanceEntry : account.getUnconfirmedAssetBalancesQNT().entrySet()) {

                JSONObject unconfirmedAssetBalance = new JSONObject();
                unconfirmedAssetBalance.put("asset", Convert.toUnsignedLong(unconfirmedAssetBalanceEntry.getKey()));
                unconfirmedAssetBalance.put("unconfirmedBalanceQNT", String.valueOf(unconfirmedAssetBalanceEntry.getValue()));
                unconfirmedAssetBalances.add(unconfirmedAssetBalance);

            }
            if (unconfirmedAssetBalances.size() > 0) {
                response.put("unconfirmedAssetBalances", unconfirmedAssetBalances);
            }
            return response;
        }
    }

}
