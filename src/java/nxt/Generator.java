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
import java.util.concurrent.TimeUnit;

public final class Generator {

    public static enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());

    private static final Runnable generateBlockThread = new Runnable() {

        private volatile int lastTimestamp;
        private volatile Long lastBlockId;

        @Override
        public void run() {

            try {
                try {
                    int timestamp = Convert.getEpochTime();
                    if (timestamp != lastTimestamp) {
                        lastTimestamp = timestamp;
                        if (Nxt.getBlockchainProcessor().isScanning()) {
                            return;
                        }
                        Block lastBlock = Nxt.getBlockchain().getLastBlock();
                        if (lastBlock.getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
                            return;
                        }
                        boolean reset = ! lastBlock.getId().equals(lastBlockId);
                        if (reset) {
                            lastBlockId = lastBlock.getId();
                        }
                        for (Generator generator : generators.values()) {
                            generator.forge(lastBlock, timestamp, reset);
                        }
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
        ThreadPool.scheduleThread(generateBlockThread, 500, TimeUnit.MILLISECONDS);
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
        if (old != null) {
            Logger.logDebugMessage("Account " + Convert.toUnsignedLong(account.getId()) + " is already forging");
            return old;
        }
        listeners.notify(generator, Event.START_FORGING);
        Logger.logDebugMessage("Account " + Convert.toUnsignedLong(account.getId()) + " started forging, deadline "
                + generator.getDeadline() + " seconds");
        return generator;
    }

    public static Generator stopForging(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            Logger.logDebugMessage("Account " + Convert.toUnsignedLong(generator.getAccountId()) + " stopped forging");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        return generator;
    }

    public static Generator getGenerator(String secretPhrase) {
        return generators.get(secretPhrase);
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }

    static boolean verifyHit(BigInteger hit, long effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(BigInteger.valueOf(effectiveBalance));
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);

        return hit.compareTo(target) < 0
                && (previousBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8
                || hit.compareTo(prevTarget) >= 0
                || (Constants.isTestnet ? elapsedTime > 300 : elapsedTime > 3600)
                || Constants.isOffline);
    }

    static long getHitTime(Account account, Block block) {
        return getHitTime(account.getEffectiveBalanceNXT(), getHit(account.getPublicKey(), block), block);
    }

    private static BigInteger getHit(byte[] publicKey, Block block) {
        if (block.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK) {
            throw new IllegalArgumentException("Not supported below Transparent Forging Block");
        }
        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(publicKey);
        return new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    private static long getHitTime(long effectiveBalanceNXT, BigInteger hit, Block block) {
        return block.getTimestamp()
                + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(effectiveBalanceNXT))).longValue();
    }


    private final Long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile long deadline;
    private volatile BigInteger hit;
    private volatile long effectiveBalance;

    private Generator(String secretPhrase, byte[] publicKey, Account account) {
        this.secretPhrase = secretPhrase;
        this.publicKey = publicKey;
        // need to store publicKey in addition to accountId, because the account may not have had its publicKey set yet
        this.accountId = account.getId();
        if (Nxt.getBlockchain().getHeight() > Constants.DIGITAL_GOODS_STORE_BLOCK) {
            forge(Nxt.getBlockchain().getLastBlock(), Convert.getEpochTime(), true); // initialize deadline
        }
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return deadline;
    }

    private void forge(Block lastBlock, int timestamp, boolean reset) {

        if (reset) {
            Account account = Account.getAccount(accountId);
            if (account == null) {
                return;
            }
            effectiveBalance = account.getEffectiveBalanceNXT();
            if (effectiveBalance <= 0) {
                return;
            }
            hit = getHit(publicKey, lastBlock);
            deadline = Math.max(getHitTime(effectiveBalance, hit, lastBlock) - timestamp, 0);
            listeners.notify(this, Event.GENERATION_DEADLINE);
        }

        if (verifyHit(hit, effectiveBalance, lastBlock, timestamp)) {
            while (! BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp)) {
                if (Convert.getEpochTime() - timestamp > 10) {
                    break;
                }
            }
        }

    }

}
