package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Generator {

    public static enum Event {
        GENERATION_DEADLINE
    }

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());

    private static final Runnable generateBlockThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {
                    for (Generator generator : generators.values()) {
                        generator.forge();
                    }
                } catch (Exception e) {
                    Logger.logDebugMessage("Error in block generation thread", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static {
        ThreadPool.scheduleThread(generateBlockThread, 1);
    }

    static void init() {}

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Generator startForging(String secretPhrase) {
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        return startForging(secretPhrase, publicKey);
    }

    public static Generator startForging(String secretPhrase, byte[] publicKey) {
        Account account = Account.getAccount(publicKey);
        if (account == null) {
            return null;
        }
        Generator generator = new Generator(secretPhrase, publicKey, account);
        Generator old = generators.putIfAbsent(secretPhrase, generator);
        return old != null ? old : generator;
    }

    public static Generator stopForging(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            lastBlocks.remove(generator.account);
            hits.remove(generator.account);
        }
        return generator;
    }

    public static Generator getGenerator(String secretPhrase) {
        return generators.get(secretPhrase);
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }

    private final Account account;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile long deadline;

    private Generator(String secretPhrase, byte[] publicKey, Account account) {
        this.secretPhrase = secretPhrase;
        this.publicKey = publicKey;
        this.account = account;
        forge(); // initialize deadline
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getDeadline() {
        return deadline;
    }

    private void forge() {

        long effectiveBalance = account.getEffectiveBalance();
        if (effectiveBalance <= 0) {
            return;
        }

        Block lastBlock = Blockchain.getLastBlock();

        if (! lastBlock.equals(lastBlocks.get(account))) {

            MessageDigest digest = Crypto.sha256();
            byte[] generationSignatureHash;
            if (lastBlock.getHeight() < Nxt.TRANSPARENT_FORGING_BLOCK) {
                byte[] generationSignature = Crypto.sign(lastBlock.getGenerationSignature(), secretPhrase);
                generationSignatureHash = digest.digest(generationSignature);
            } else {
                digest.update(lastBlock.getGenerationSignature());
                generationSignatureHash = digest.digest(publicKey);
            }

            BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            lastBlocks.put(account, lastBlock);
            hits.put(account, hit);

            long total = hit.divide(BigInteger.valueOf(lastBlock.getBaseTarget()).multiply(BigInteger.valueOf(account.getEffectiveBalance()))).longValue();
            long elapsed = Math.max(Convert.getEpochTime() - lastBlock.getTimestamp(), 0);

            deadline = total - elapsed;

            listeners.notify(this, Event.GENERATION_DEADLINE);

        }

        int elapsedTime = Convert.getEpochTime() - lastBlock.getTimestamp();
        if (elapsedTime > 0) {
            BigInteger target = BigInteger.valueOf(lastBlock.getBaseTarget()).multiply(BigInteger.valueOf(effectiveBalance)).multiply(BigInteger.valueOf(elapsedTime));
            if (hits.get(account).compareTo(target) < 0) {
                Blockchain.generateBlock(secretPhrase);
            }
        }

    }

}
