package nxt.user;

import nxt.Account;
import nxt.util.JSON;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public class User {

    final ConcurrentLinkedQueue<JSONObject> pendingResponses;
    private AsyncContext asyncContext;
    volatile boolean isInactive;

    public volatile String secretPhrase;
    public volatile byte[] publicKey;

    User() {

        pendingResponses = new ConcurrentLinkedQueue<>();

    }

    public static void updateUserUnconfirmedBalance(Account account) {

        JSONObject response = new JSONObject();
        response.put("response", "setBalance");
        response.put("balance", account.getUnconfirmedBalance());
        byte[] accountPublicKey = account.publicKey.get();
        for (User user : Nxt.users.values()) {

            if (user.secretPhrase != null && Arrays.equals(user.publicKey, accountPublicKey)) {

                user.send(response);

            }

        }

    }

    void deinitializeKeyPair() {

        secretPhrase = null;
        publicKey = null;

    }

    BigInteger initializeKeyPair(String secretPhrase) {

        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.secretPhrase = secretPhrase;
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4], publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});

    }

    public synchronized void send(JSONObject response) {
        if (asyncContext == null) {

            if (isInactive) {
                // user not seen recently, no responses should be collected
                return;
            }
            if (pendingResponses.size() > 1000) {
                pendingResponses.clear();
                // stop collecting responses for this user
                isInactive = true;
                if (secretPhrase == null) {
                    // but only completely remove users that don't have unlocked accounts
                    Nxt.users.values().remove(this);
                }
                return;
            }

            pendingResponses.offer(response);

        } else {

            JSONArray responses = new JSONArray();
            JSONObject pendingResponse;
            while ((pendingResponse = pendingResponses.poll()) != null) {

                responses.add(pendingResponse);

            }
            responses.add(response);

            JSONObject combinedResponse = new JSONObject();
            combinedResponse.put("responses", responses);

            asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

            try (Writer writer = asyncContext.getResponse().getWriter()) {
                combinedResponse.writeJSONString(writer);
            } catch (IOException e) {
                Logger.logMessage("Error sending response to user", e);
            }

            asyncContext.complete();
            asyncContext = null;

        }

    }

    public synchronized void processPendingResponses(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONArray responses = new JSONArray();
        JSONObject pendingResponse;
        while ((pendingResponse = pendingResponses.poll()) != null) {
            responses.add(pendingResponse);
        }
        if (responses.size() > 0) {
            JSONObject combinedResponse = new JSONObject();
            combinedResponse.put("responses", responses);
            if (asyncContext != null) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    combinedResponse.writeJSONString(writer);
                }
                asyncContext.complete();
                asyncContext = req.startAsync();
                asyncContext.addListener(new UserAsyncListener());
                asyncContext.setTimeout(5000);
            } else {
                resp.setContentType("text/plain; charset=UTF-8");
                try (Writer writer = resp.getWriter()) {
                    combinedResponse.writeJSONString(writer);
                }
            }
        } else {
            if (asyncContext != null) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    JSON.emptyJSON.writeJSONString(writer);
                }
                asyncContext.complete();
            }
            asyncContext = req.startAsync();
            asyncContext.addListener(new UserAsyncListener());
            asyncContext.setTimeout(5000);
        }
    }

    private final class UserAsyncListener implements AsyncListener {

        @Override
        public void onComplete(AsyncEvent asyncEvent) throws IOException { }

        @Override
        public void onError(AsyncEvent asyncEvent) throws IOException {

            synchronized (User.this) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    JSON.emptyJSON.writeJSONString(writer);
                }

                asyncContext.complete();
                asyncContext = null;
            }

        }

        @Override
        public void onStartAsync(AsyncEvent asyncEvent) throws IOException { }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {

            synchronized (User.this) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    JSON.emptyJSON.writeJSONString(writer);
                }

                asyncContext.complete();
                asyncContext = null;
            }

        }

    }

}
