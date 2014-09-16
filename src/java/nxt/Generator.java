package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Generator implements Comparable<Generator> {

    public static enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
    private static volatile List<Generator> sortedForgers;

    private static final Runnable generateBlockThread = new Runnable() {

        private volatile int lastTimestamp;
        private volatile Long lastBlockId;

        @Override
        public void run() {

            try {
                try {
                    int timestamp = Convert.getEpochTime();
                    if (timestamp == lastTimestamp) {
                        return;
                    }
                    lastTimestamp = timestamp;
                    synchronized (Nxt.getBlockchain()) {
                        Block lastBlock = Nxt.getBlockchain().getLastBlock();
                        if (lastBlock == null || lastBlock.getHeight() < Constants.DIGITAL_GOODS_STORE_BLOCK) {
                            return;
                        }
                        if (!lastBlock.getId().equals(lastBlockId) || sortedForgers == null) {
                            lastBlockId = lastBlock.getId();
                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.setLastBlock(lastBlock);
                                if (generator.effectiveBalance.signum() > 0) {
                                    forgers.add(generator);
                                }
                            }
                            Collections.sort(forgers);
                            sortedForgers = Collections.unmodifiableList(forgers);
                        }
                        for (Generator generator : sortedForgers) {
                            if (generator.getHitTime() > timestamp + 1 || generator.forge(lastBlock, timestamp)) {
                                return;
                            }
                        }
                    } // synchronized
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
        Generator generator = new Generator(secretPhrase);
        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            Logger.logDebugMessage("Account " + Convert.toUnsignedLong(old.getAccountId()) + " is already forging");
            return old;
        }
        listeners.notify(generator, Event.START_FORGING);
        Logger.logDebugMessage("Account " + Convert.toUnsignedLong(generator.getAccountId()) + " started forging, deadline "
                + generator.getDeadline() + " seconds");
        return generator;
    }

    public static Generator stopForging(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            sortedForgers = null;
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

    static boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (Nxt.isIsUnitTest()) {
            if (elapsedTime < 0) {
                return false;
            }
        } else {
            if (elapsedTime <= 0) {
                return false;
            }
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        return Nxt.isIsUnitTest() || (hit.compareTo(target) < 0
                && (previousBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8
                || hit.compareTo(prevTarget) >= 0
                || (Constants.isTestnet ? elapsedTime > 300 : elapsedTime > 3600)
                || Constants.isOffline));
    }

    static long getHitTime(Account account, Block block) {
        return getHitTime(BigInteger.valueOf(account.getEffectiveBalanceNXT()), getHit(account.getPublicKey(), block), block);
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

    private static long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
        return block.getTimestamp()
                + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();
    }


    private final Long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile long hitTime;
    private volatile BigInteger hit;
    private volatile BigInteger effectiveBalance;

    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
        if (Nxt.getBlockchain().getHeight() > Constants.DIGITAL_GOODS_STORE_BLOCK) {
            setLastBlock(Nxt.getBlockchain().getLastBlock());
        }
        sortedForgers = null;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return Math.max(hitTime - Nxt.getBlockchain().getLastBlock().getTimestamp(), 0);
    }

    public long getHitTime() {
        return hitTime;
    }

    @Override
    public int compareTo(Generator g) {
        int i = this.hit.multiply(g.effectiveBalance).compareTo(g.hit.multiply(this.effectiveBalance));
        if (i != 0) {
            return i;
        }
        return accountId.compareTo(g.accountId);
    }

    @Override
    public String toString() {
        return "account: " + Convert.toUnsignedLong(accountId) + " deadline: " + getDeadline();
    }

    private void setLastBlock(Block lastBlock) {
        Account account = Account.getAccount(accountId);
        effectiveBalance = BigInteger.valueOf(account == null || account.getEffectiveBalanceNXT() <= 0 ? 0 : account.getEffectiveBalanceNXT());
        if (effectiveBalance.signum() == 0) {
            return;
        }
        hit = getHit(publicKey, lastBlock);
        hitTime = getHitTime(effectiveBalance, hit, lastBlock);
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    private boolean forge(Block lastBlock, int timestamp) throws BlockchainProcessor.BlockNotAcceptedException {
        if (verifyHit(hit, effectiveBalance, lastBlock, timestamp)) {
            while (true) {
                try {
                    BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp);
                    return true;
                } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                    if (Convert.getEpochTime() - timestamp > 10) {
                        throw e;
                    }
                }
            }
        }
        return false;
    }

}
