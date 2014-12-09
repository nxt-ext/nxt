package nxt.mint;

import nxt.Account;
import nxt.Constants;
import nxt.CurrencyMint;
import nxt.Nxt;
import nxt.crypto.Crypto;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MintWorker {

    public static void main(String[] args) {
        MintWorker mintWorker = new MintWorker();
        mintWorker.mint();
    }

    private void mint() {
        String currencyCode = Convert.emptyToNull(Nxt.getStringProperty("nxt.mint.currencyCode"));
        if (currencyCode == null) {
            throw new IllegalArgumentException("nxt.mint.currencyCode not specified");
        }
        String secretPhrase = Convert.emptyToNull(Nxt.getStringProperty("nxt.mint.secretPhrase"));
        if (secretPhrase == null) {
            throw new IllegalArgumentException("nxt.mint.secretPhrase not specified");
        }
        long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        String rsAccount = Convert.rsAccount(accountId);
        JSONObject currency = getCurrency(currencyCode);
        if (currency.get("currency") == null) {
            throw new IllegalArgumentException("Invalid currency code " + currencyCode);
        }
        long currencyId = Convert.parseUnsignedLong((String) currency.get("currency"));
        if (currency.get("algorithm") == null) {
            throw new IllegalArgumentException("Minting algorithm not specified, currency " + currencyCode + " is not mintable");
        }
        byte algorithm = (byte)(long) currency.get("algorithm");
        byte decimal = (byte)(long) currency.get("decimals");
        String unitsStr = Nxt.getStringProperty("nxt.mint.unitsPerMint");
        double wholeUnits = 1;
        if (unitsStr != null && unitsStr.length() > 0) {
            wholeUnits = Double.parseDouble(unitsStr);
        }
        long units = (long)(wholeUnits * Math.pow(10, decimal));
        JSONObject mintingTarget = getMintingTarget(currencyCode, rsAccount, units);
        long counter = (long) mintingTarget.get("counter");
        byte[] target = Convert.parseHexString((String) mintingTarget.get("targetBytes"));
        long difficulty = (long) mintingTarget.get("difficulty");
        long initialNonce = Nxt.getIntProperty("nxt.mint.initialNonce");
        if (initialNonce == 0) {
            initialNonce = new Random().nextLong();
        }
        int threadPoolSize = Nxt.getIntProperty("nxt.mint.threadPoolSize");
        if (threadPoolSize == 0) {
            threadPoolSize = Runtime.getRuntime().availableProcessors();
            Logger.logDebugMessage("Thread pool size " + threadPoolSize);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        while (true) {
            counter++;
            JSONObject response = mintImpl(currencyCode, secretPhrase, accountId, units, currencyId, algorithm, counter, target,
                    initialNonce, threadPoolSize, executorService, difficulty);
            Logger.logDebugMessage("currency mint response:" + response.toJSONString());
            if (response.get("error") != null || response.get("errorCode") != null) {
                break;
            }
        }
    }

    private JSONObject mintImpl(String currencyCode, String secretPhrase, long accountId, long units, long currencyId, byte algorithm,
                                long counter, byte[] target, long initialNonce, int threadPoolSize, ExecutorService executorService, long difficulty) {
        long startTime = System.currentTimeMillis();
        List<Callable<Long>> workersList = new ArrayList<>();
        for (int i=0; i < threadPoolSize; i++) {
            HashSolver hashSolver = new HashSolver(algorithm, currencyId, accountId, counter, units, initialNonce + i, target, threadPoolSize);
            workersList.add(hashSolver);
        }
        long solution = solve(executorService, workersList);
        long computationTime = System.currentTimeMillis() - startTime;
        long hashes = solution - initialNonce;
        Logger.logDebugMessage("solution nonce %d computed hashes %d rate vs. expected %.2f time %d [KH/Sec] %d",
                solution, hashes, (float)hashes/(float)difficulty, computationTime, hashes/computationTime);
        return currencyMint(secretPhrase, currencyCode, initialNonce, units, counter);
    }

    private long solve(Executor executor, Collection<Callable<Long>> solvers) {
        CompletionService<Long> ecs = new ExecutorCompletionService<>(executor);
        List<Future<Long>> futures = new ArrayList<>(solvers.size());
        for (Callable<Long> solver : solvers) {
            futures.add(ecs.submit(solver));
        }
        try {
            return ecs.take().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            for (Future<Long> f : futures) {
                f.cancel(true);
            }
        }
    }

    private JSONObject currencyMint(String secretPhrase, String currencyCode, long nonce, long units, long counter) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "currencyMint");
        params.put("secretPhrase", secretPhrase);
        params.put("feeNQT", Long.toString(Constants.ONE_NXT));
        params.put("deadline", Integer.toString(1440));
        params.put("code", currencyCode);
        params.put("nonce", Long.toString(nonce));
        params.put("units", Long.toString(units));
        params.put("counter", Long.toString(counter));
        return getJsonResponse(params);
    }

    private JSONObject getCurrency(String currencyCode) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getCurrency");
        params.put("code", currencyCode);
        return getJsonResponse(params);
    }

    private JSONObject getMintingTarget(String currencyCode, String rsAccount, long units) {
        Map<String, String> params = new HashMap<>();
        params.put("requestType", "getMintingTarget");
        params.put("code", currencyCode);
        params.put("account", rsAccount);
        params.put("units", Long.toString(units));
        return getJsonResponse(params);
    }

    private JSONObject getJsonResponse(Map<String, String> params) {
        JSONObject response;
        HttpURLConnection connection = null;
        String host = Nxt.getStringProperty("nxt.myAddress");
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                host = "localhost";
            }
        }

        int port = Constants.isTestnet ? API.TESTNET_API_PORT : Nxt.getIntProperty("nxt.apiServerPort");
        String urlParams = getUrlParams(params);
        try {
            URL url = new URL("http", host, port, "/nxt?" + urlParams);
            Logger.logDebugMessage("Sending request to server: " + url.toString());
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
            try {
                sb.append(key).append("=").append(URLEncoder.encode(params.get(key), "utf8")).append("&");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
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
        private final byte[] target;
        private final int poolSize;

        private HashSolver(byte algorithm, long currencyId, long accountId, long counter, long units, long nonce,
                           byte[] target, int poolSize) {
            this.hashFunction = HashFunction.getHashFunction(algorithm);
            this.currencyId = currencyId;
            this.accountId = accountId;
            this.counter = counter;
            this.units = units;
            this.nonce = nonce;
            this.target = target;
            this.poolSize = poolSize;
        }

        @Override
        public Long call() {
            long n = nonce;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] hash = CurrencyMint.getHash(hashFunction, n, currencyId, units, counter, accountId);
                if (CurrencyMint.meetsTarget(hash, target)) {
                    return n;
                }
                n+=poolSize;
                if (((n - nonce) % (poolSize * 1000000)) == 0) {
                    Logger.logDebugMessage("%s computed %d [MH]", Thread.currentThread().getName(), (n - nonce) / poolSize / 1000000);
                }
            }
            return null;
        }
    }
}