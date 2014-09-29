package nxt;

import nxt.util.Listener;
import nxt.util.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class BlockchainProcessorTest {

    private static final String defaultTraceFile = "nxt-trace-default.csv";
    private static final String testTraceFile = "nxt-trace.csv";
    private static final int maxHeight = 245000;

    private static BlockchainProcessorImpl blockchainProcessor;
    private static BlockchainImpl blockchain;
    private static DebugTrace debugTrace;
    private static final Object downloadLock = new Object();
    private static boolean done = false;

    private static Properties newTestProperties() {
        Properties testProperties = new Properties();
        testProperties.setProperty("nxt.shareMyAddress", "false");
        testProperties.setProperty("nxt.savePeers", "false");
        testProperties.setProperty("nxt.enableAPIServer", "false");
        testProperties.setProperty("nxt.enableUIServer", "false");
        testProperties.setProperty("nxt.disableGenerateBlocksThread", "true");
        testProperties.setProperty("nxt.disableProcessTransactionsThread", "true");
        testProperties.setProperty("nxt.disableRemoveUnconfirmedTransactionsThread", "true");
        testProperties.setProperty("nxt.disableRebroadcastTransactionsThread", "true");
        testProperties.setProperty("nxt.disablePeerUnBlacklistingThread", "true");
        testProperties.setProperty("nxt.getMorePeers", "false");
        testProperties.setProperty("nxt.debugTraceAccounts", "");
        testProperties.setProperty("nxt.debugLogUnconfirmed", "false");
        testProperties.setProperty("nxt.debugTraceQuote", "\"");
        return testProperties;
    }

    @BeforeClass
    public static void init() {
        Nxt.init(newTestProperties());
        blockchain = BlockchainImpl.getInstance();
        blockchainProcessor = BlockchainProcessorImpl.getInstance();
        blockchainProcessor.setGetMoreBlocks(false);
        debugTrace = DebugTrace.addDebugTrace(Collections.<Long>emptySet(), testTraceFile);
        Listener<Block> countingListener = new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (block.getHeight() % 1000 == 0) {
                    Logger.logMessage("downloaded block " + block.getHeight());
                }
            }
        };
        blockchainProcessor.addListener(countingListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    @AfterClass
    public static void shutdown() {
        Nxt.shutdown();
    }

    @Before
    public void reset() {
        debugTrace.resetLog();
        blockchainProcessor.fullReset();
        Assert.assertEquals(0, blockchain.getHeight());
    }

    @Test
    public void fullDownloadAndRescanTest() {
        download(0, maxHeight);
        rescan(blockchain.getHeight());
    }

    @Test
    public void multipleRescanTest() {
        int start = 0;
        int end;
        while ((end = start + 2000) <= maxHeight) {
            download(start, end);
            rescan(500);
            rescan(900);
            rescan(720);
            rescan(1439);
            rescan(200);
            rescan(1);
            rescan(2);
            start = end;
        }
    }

    @Test
    public void multiplePopOffTest() {
        int start = 0;
        int end;
        while ((end = start + 2000) <= maxHeight) {
            download(start, end);
            popOff(800);
            popOff(1440);
            popOff(720);
            popOff(1);
            start = end;
        }
    }

    private static void download(final int startHeight, final int endHeight) {
        Assert.assertEquals(startHeight, blockchain.getHeight());
        downloadTo(endHeight);
        Logger.logMessage("Successfully downloaded blockchain from " + startHeight + " to " + endHeight);
        compareTraceFiles();
        debugTrace.resetLog();
    }

    private static void rescan(final int numBlocks) {
        int endHeight = blockchain.getHeight();
        int rescanHeight = endHeight - numBlocks;
        blockchainProcessor.validateAtNextScan();
        blockchainProcessor.scan(rescanHeight);
        Assert.assertEquals(endHeight, blockchain.getHeight());
        Logger.logMessage("Successfully rescanned blockchain from " + rescanHeight + " to " + endHeight);
        compareTraceFiles();
        debugTrace.resetLog();
    }

    private static void popOff(final int numBlocks) {
        int endHeight = blockchain.getHeight();
        blockchainProcessor.popOffTo(endHeight - numBlocks);
        Assert.assertEquals(endHeight - numBlocks, blockchain.getHeight());
        downloadTo(endHeight);
        Logger.logMessage("Successfully redownloaded blockchain from " + (endHeight - numBlocks) + " to " + endHeight);
        compareTraceFiles();
        debugTrace.resetLog();
    }

    private static void downloadTo(final int endHeight) {
        Assert.assertTrue(blockchain.getHeight() <= endHeight);
        Listener<Block> stopListener = new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (blockchain.getHeight() == endHeight) {
                    synchronized (downloadLock) {
                        done = true;
                        downloadLock.notifyAll();
                    }
                    throw new NxtException.StopException("Reached height " + endHeight);
                }
            }
        };
        blockchainProcessor.addListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
        synchronized (downloadLock) {
            done = false;
            Logger.logMessage("Starting download from height " + blockchain.getHeight());
            blockchainProcessor.setGetMoreBlocks(true);
            while (! done) {
                try {
                    downloadLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Assert.assertEquals(endHeight, blockchain.getHeight());
        blockchainProcessor.removeListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    private static void compareTraceFiles() {
        try (BufferedReader defaultReader = new BufferedReader(new FileReader(defaultTraceFile));
             BufferedReader testReader = new BufferedReader(new FileReader(testTraceFile))) {
            defaultReader.readLine();
            testReader.readLine();
            String testLine = testReader.readLine();
            if (testLine == null) {
                Logger.logMessage("Empty trace file, nothing to compare");
                return;
            }
            int startHeight = parseHeight(testLine);
            String defaultLine;
            while ((defaultLine = defaultReader.readLine()) != null) {
                if (parseHeight(defaultLine) >= startHeight) {
                    break;
                }
            }
            Assert.assertEquals(defaultLine, testLine);
            while ((testLine = testReader.readLine()) != null) {
                Assert.assertEquals(defaultLine = defaultReader.readLine(), testLine);
            }
            Logger.logMessage("Comparison with default trace file passed from height " + startHeight + " to " + parseHeight(defaultLine));
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static int parseHeight(String line) {
        return Integer.parseInt(line.substring(1, line.indexOf(DebugTrace.SEPARATOR) - 1));
    }

}
