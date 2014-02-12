package nxt;

import nxt.peer.Peer;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPools {

    private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(8);
    private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);

    public static Future<JSONObject> sendInParallel(final Peer peer, final JSONStreamAware jsonRequest) {
        return sendToPeersService.submit(new Callable<JSONObject>() {
            @Override
            public JSONObject call() {
                return peer.send(jsonRequest);
            }
        });
    }

    static void start() {

        scheduledThreadPool.scheduleWithFixedDelay(Peer.peerConnectingThread, 0, 5, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Peer.peerUnBlacklistingThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Peer.getMorePeersThread, 0, 5, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.processTransactionsThread, 0, 5, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.removeUnconfirmedTransactionsThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.getMoreBlocksThread, 0, 1, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Blockchain.rebroadcastTransactionsThread, 0, 60, TimeUnit.SECONDS);

        scheduledThreadPool.scheduleWithFixedDelay(Generator.generateBlockThread, 0, 1, TimeUnit.SECONDS);

    }

    static void shutdown() {
        shutdownExecutor(scheduledThreadPool);
        shutdownExecutor(sendToPeersService);
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

    private ThreadPools() {} //never

}
