package nxt.addons;

import nxt.Account;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Db;
import nxt.FxtDistribution;
import nxt.Nxt;
import nxt.NxtException;
import nxt.http.APIServlet;
import nxt.http.APITag;
import nxt.http.JSONResponses;
import nxt.http.ParameterParser;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class JPLSnapshot implements AddOn {

    public APIServlet.APIRequestHandler getAPIRequestHandler() {
        return new JPLSnapshotAPI("file", new APITag[] {APITag.ADDONS}, "height");
    }

    public String getAPIRequestType() {
        return "downloadJPLSnapshot";
    }


    private static class JPLSnapshotAPI extends APIServlet.APIRequestHandler {

        private JPLSnapshotAPI(String fileParameter, APITag[] apiTags, String... origParameters) {
            super(fileParameter, apiTags, origParameters);
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws NxtException {
            int height = ParameterParser.getHeight(request);
            if (height <= 0) {
                return JSONResponses.INCORRECT_HEIGHT;
            }
            JSONObject inputJSON;
            try {
                Part part = request.getPart("file");
                if (part != null) {
                    ParameterParser.FileData fileData = new ParameterParser.FileData(part).invoke();
                    inputJSON = (JSONObject)JSONValue.parse(Convert.toString(fileData.getData()));
                } else {
                    inputJSON = new JSONObject();
                }
            } catch (IOException | ServletException e) {
                return JSONResponses.INCORRECT_FILE;
            }
            JPLSnapshotListener listener = new JPLSnapshotListener(height, inputJSON);
            Nxt.getBlockchainProcessor().addListener(listener, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
            Nxt.getBlockchainProcessor().scan(height - 1, false);
            Nxt.getBlockchainProcessor().removeListener(listener, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
            StringBuilder sb = new StringBuilder(1024);
            JSON.encodeObject(listener.getSnapshot(), sb);
            response.setHeader("Content-Disposition", "attachment; filename=genesisAccounts.json");
            response.setContentLength(sb.length());
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                writer.write(sb.toString());
            } catch (IOException e) {
                return JSONResponses.RESPONSE_WRITE_ERROR;
            }
            return null;
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean requirePost() {
            return true;
        }

        @Override
        protected boolean requirePassword() {
            return true;
        }

        @Override
        protected boolean requireFullClient() {
            return true;
        }
    }

    private static class JPLSnapshotListener implements Listener<Block> {

        private final int height;
        private final JSONObject inputJSON;
        private final SortedMap<String, Object> snapshot = new TreeMap<>();

        private JPLSnapshotListener(int height, JSONObject inputJSON) {
            this.height = height;
            this.inputJSON = inputJSON;
        }

        @Override
        public void notify(Block block) {
            if (block.getHeight() == height) {
                SortedMap<String, String> snapshotPublicKeys = snapshotPublicKeys();
                JSONArray inputPublicKeys = (JSONArray)inputJSON.get("publicKeys");
                if (inputPublicKeys != null) {
                    inputPublicKeys.forEach(publicKey -> {
                        String account = Long.toUnsignedString(Account.getId(Convert.parseHexString((String)publicKey)));
                        String snapshotPublicKey = snapshotPublicKeys.putIfAbsent(account, (String)publicKey);
                        if (snapshotPublicKey != null && !snapshotPublicKey.equals(publicKey)) {
                            throw new RuntimeException("Public key collision, input " + publicKey + ", snapshot contains " + snapshotPublicKey);
                        }
                    });
                }
                JSONArray publicKeys = new JSONArray();
                publicKeys.addAll(snapshotPublicKeys.values());
                snapshot.put("publicKeys", publicKeys);
                SortedMap<String, Long> snapshotNxtBalances = snapshotNxtBalances();
                BigInteger snapshotTotal = BigInteger.valueOf(snapshotNxtBalances.values().stream().mapToLong(Long::longValue).sum());
                JSONObject inputBalances = (JSONObject)inputJSON.get("balances");
                if (inputBalances != null) {
                    BigInteger inputTotal = BigInteger.valueOf(inputBalances.values().stream().mapToLong(value -> (Long) value).sum());
                    if (!inputTotal.equals(BigInteger.ZERO)) {
                        snapshotNxtBalances.entrySet().forEach(entry -> {
                            long snapshotBalance = entry.getValue();
                            long adjustedBalance = BigInteger.valueOf(snapshotBalance).multiply(inputTotal)
                                    .divide(snapshotTotal).divide(BigInteger.valueOf(9)).longValueExact();
                            entry.setValue(adjustedBalance);
                        });
                    }
                    inputBalances.entrySet().forEach(entry -> {
                        long accountId = Convert.parseAccountId((String)((Map.Entry)entry).getKey());
                        String account = Long.toUnsignedString(accountId);
                        long inputBalance = (Long)((Map.Entry)entry).getValue();
                        snapshotNxtBalances.merge(account, inputBalance, (a, b) -> a + b);
                    });
                }
                snapshot.put("balances", snapshotNxtBalances);
            }
        }

        private SortedMap<String, Object> getSnapshot() {
            return snapshot;
        }

        private SortedMap<String, String> snapshotPublicKeys() {
            SortedMap<String, String> map = new TreeMap<>();
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT public_key FROM public_key WHERE public_key IS NOT NULL "
                         + "AND height <= ? ORDER by account_id")) {
                pstmt.setInt(1, height);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        byte[] publicKey = rs.getBytes("public_key");
                        long accountId = Account.getId(publicKey);
                        map.put(Long.toUnsignedString(accountId), Convert.toHexString(publicKey));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return map;
        }

        private SortedMap<String, Long> snapshotNxtBalances() {
            SortedMap<String, Long> map = new TreeMap<>();
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT id, balance FROM account WHERE LATEST=true")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long accountId = rs.getLong("id");
                        if (accountId == FxtDistribution.FXT_ISSUER_ID) {
                            continue;
                        }
                        long balance = rs.getLong("balance");
                        if (balance <= 0) {
                            continue;
                        }
                        String account = Long.toUnsignedString(accountId);
                        map.put(account, balance);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return map;
        }
    }
}
