package nxt;

import nxt.crypto.Crypto;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

class User {

    final ConcurrentLinkedQueue<JSONObject> pendingResponses;
    AsyncContext asyncContext;
    volatile boolean isInactive;

    volatile String secretPhrase;
    volatile byte[] publicKey;

    User() {

        pendingResponses = new ConcurrentLinkedQueue<>();

    }

    static void updateUserUnconfirmedBalance(Account account) {

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

    void send(JSONObject response) {

        synchronized (this) {

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

    }

    final class UserAsyncListener implements AsyncListener {

        @Override
        public void onComplete(AsyncEvent asyncEvent) throws IOException { }

        @Override
        public void onError(AsyncEvent asyncEvent) throws IOException {

            synchronized (User.this) {
                asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");

                try (Writer writer = asyncContext.getResponse().getWriter()) {
                    new JSONObject().writeJSONString(writer);
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
                    new JSONObject().writeJSONString(writer);
                }

                asyncContext.complete();
                asyncContext = null;
            }

        }

    }
}
