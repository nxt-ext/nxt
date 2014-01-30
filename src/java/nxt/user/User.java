package nxt.user;

import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public final class User {

    private static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());

    public static Collection<User> getAllUsers() {
        return allUsers;
    }

    public static User getUser(String userPasscode) {
        User user = users.get(userPasscode);
        if (user == null) {
            user = new User();
            User oldUser = users.putIfAbsent(userPasscode, user);
            if (oldUser != null) {
                user = oldUser;
                user.isInactive = false;
            }
        } else {
            user.isInactive = false;
        }
        return user;
    }

    public static void sendToAll(JSONStreamAware response) {
        for (User user : User.users.values()) {
            user.send(response);
        }
    }

    public static void updateUserUnconfirmedBalance(Account account) {
        JSONObject response = new JSONObject();
        response.put("response", "setBalance");
        response.put("balance", account.getUnconfirmedBalance());
        byte[] accountPublicKey = account.getPublicKey();
        for (User user : users.values()) {
            if (user.secretPhrase != null && Arrays.equals(user.publicKey, accountPublicKey)) {
                user.send(response);
            }
        }
    }

    private volatile String secretPhrase;
    private volatile byte[] publicKey;
    private volatile boolean isInactive;
    private final ConcurrentLinkedQueue<JSONStreamAware> pendingResponses = new ConcurrentLinkedQueue<>();
    private AsyncContext asyncContext;

    User() {}

    public String getSecretPhrase() {
        return secretPhrase;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    boolean isInactive() {
        return isInactive;
    }

    public synchronized void send(JSONStreamAware response) {
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
                    users.values().remove(this);
                }
                return;
            }

            pendingResponses.offer(response);

        } else {

            JSONArray responses = new JSONArray();
            JSONStreamAware pendingResponse;
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
        JSONStreamAware pendingResponse;
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

    void enqueue(JSONStreamAware response) {
        pendingResponses.offer(response);
    }

    void deinitializeKeyPair() {
        secretPhrase = null;
        publicKey = null;
    }

    BigInteger initializeKeyPair(String secretPhrase) {
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.secretPhrase = secretPhrase;
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return new BigInteger(1, new byte[] {publicKeyHash[7], publicKeyHash[6], publicKeyHash[5], publicKeyHash[4],
                publicKeyHash[3], publicKeyHash[2], publicKeyHash[1], publicKeyHash[0]});
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
