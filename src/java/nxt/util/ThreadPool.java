package nxt.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPool {

    private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(8);

    public static void scheduleThread(Runnable runnable, int delay) {
        scheduledThreadPool.scheduleWithFixedDelay(runnable, 0, delay, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        shutdownExecutor(scheduledThreadPool);
    }

    public static void shutdownExecutor(ExecutorService executor) {
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

    private ThreadPool() {} //never

}
