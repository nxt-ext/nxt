package nxt;

import nxt.peer.Peer;
import nxt.util.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPools {

    private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(8);
    private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);

    static void start() {

        scheduledThreadPool.scheduleWithFixedDelay(Peer.peerConnectingThread, 0, 5, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Peer.peerUnBlacklistingThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Peer.getMorePeersThread, 0, 5, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.processTransactionsThread, 0, 5, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.removeUnconfirmedTransactionsThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.getMoreBlocksThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.generateBlockThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.rebroadcastTransactionsThread, 0, 60, TimeUnit.SECONDS);

    }

    static void shutdown() {
        shutdownExecutor(scheduledThreadPool);
        shutdownExecutor(sendToPeersService);
    }

    public static <T> Future<T> sendToPeers(Callable<T> callable) {
        return sendToPeersService.submit(callable);
    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! executor.isTerminated()) {
            Logger.logMessage("some threads didn't terminate, forcing shutdown");
            executor.shutdownNow();
        }
    }

}
