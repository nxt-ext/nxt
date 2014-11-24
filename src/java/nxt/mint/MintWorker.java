package nxt.mint;

import nxt.Constants;
import nxt.CurrencyMint;
import nxt.Nxt;
import nxt.crypto.HashFunction;
import nxt.http.API;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MintWorker {

    public static void main(String[] args) {
        MintWorker mintWorker = new MintWorker();
        mintWorker.mint();
    }

    private void mint() {
        String currencyCode = Nxt.getStringProperty("nxt.mint.currencyCode");
        long accountId = Convert.parseAccountId(Nxt.getStringProperty("nxt.mint.accountId"));
        long units = Convert.parseAccountId(Nxt.getStringProperty("nxt.mint.unitsPerMint"));
        JSONObject currency = getCurrency(currencyCode);
        long currencyId = Convert.parseUnsignedLong((String) currency.get("currency"));
        byte algorithm = (byte) currency.get("algorithm");
        JSONObject mintingTarget = getMintingTarget(currencyCode, units);
        long counter = (long) currency.get("counter");
        counter++;
        byte[] target = Convert.parseHexString((String) mintingTarget.get("targetBytes"));
        BigInteger difficulty = new BigInteger((String) mintingTarget.get("difficulty"));
        Logger.logDebugMessage("difficulty:" + difficulty);
        long nonce = new Random().nextLong();
        long batchSize = 10000;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Long result = null;
        while (result == null) {
            HashSolver hashSolver = new HashSolver(algorithm, currencyId, accountId, counter, units, nonce, batchSize, target);
            Future<Long> future = executorService.submit(hashSolver);
            try {
                result = future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
            nonce += batchSize;
        }
        Logger.logDebugMessage("nonce:" + nonce);
    }

    private JSONObject getCurrency(String currencyCode) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getCurrency");
        params.put("code", currencyCode);
        return getJsonResponse(params);
    }

    private JSONObject getMintingTarget(String currencyCode, long units) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getMintingTarget");
        params.put("code", currencyCode);
        params.put("units", Long.toString(units));
        return getJsonResponse(params);
    }

    private JSONObject getJsonResponse(Map<String, String> params) {
        JSONObject response;
        HttpURLConnection connection = null;
        final String host = Nxt.getStringProperty("nxt.apiServerHost");
        final int port = Constants.isTestnet ? API.TESTNET_API_PORT : Nxt.getIntProperty("nxt.apiServerPort");
        String urlParams = getUrlParams(params);
        String spec = "http://" + host + ":" + port + "/nxt?" + urlParams;
        try {
            URL url = new URL(spec); // TODO use standard URL builder
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    response = (JSONObject) JSONValue.parse(reader);
                }
            } else {
                response = null;
            }
        } catch (RuntimeException | IOException e) {
            Logger.logInfoMessage("Error connecting to server", e);
            if (connection != null) {
                connection.disconnect();
            }
            throw new IllegalStateException(e);
        }
        return response;
    }

    private static String getUrlParams(Map<String, String> params) {
        if (params == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : params.keySet()) {
            sb.append(key).append("=").append(params.get(key)).append("&");
        }
        String rc = sb.toString();
        if (rc.endsWith("&")) {
            rc = rc.substring(0, rc.length() - 1);
        }
        return rc;
    }

    private static class HashSolver implements Callable<Long> {

        private final HashFunction hashFunction;
        private final long currencyId;
        private final long accountId;
        private final long counter;
        private final long units;
        private final long nonce;
        private final long attempts;
        private final byte[] target;

        private HashSolver(byte algorithm, long currencyId, long accountId, long counter, long units, long nonce, long attempts, byte[] target) {
            this.hashFunction = HashFunction.getHashFunction(algorithm);
            this.currencyId = currencyId;
            this.accountId = accountId;
            this.counter = counter;
            this.units = units;
            this.nonce = nonce;
            this.attempts = attempts;
            this.target = target;
        }

        @Override
        public Long call() {
            long n = nonce;
            while (n < attempts) {
                byte[] hash = CurrencyMint.getHash(hashFunction, n, currencyId, units, counter, accountId);
                if (CurrencyMint.meetsTarget(hash, target)) {
                    return n;
                }
                n++;
            }
            return null;
        }
    }
}