package nxt;

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

public class BlockchainProcessorTest extends AbstractBlockchainTest {

    private static final String defaultTraceFile = "nxt-trace-default.csv";
    private static final String testTraceFile = "nxt-trace.csv";
    private static final int maxHeight = 245000;

    private static DebugTrace debugTrace;

    @BeforeClass
    public static void init() {
        AbstractBlockchainTest.init(newTestProperties());
        debugTrace = DebugTrace.addDebugTrace(Collections.<Long>emptySet(), BlockchainProcessorTest.testTraceFile);
    }

    @AfterClass
    public static void shutdown() {
        AbstractBlockchainTest.shutdown();
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
            redownload(800);
            redownload(1440);
            redownload(720);
            redownload(1);
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

    private static void redownload(final int numBlocks) {
        int endHeight = blockchain.getHeight();
        blockchainProcessor.popOffTo(endHeight - numBlocks);
        Assert.assertEquals(endHeight - numBlocks, blockchain.getHeight());
        downloadTo(endHeight);
        Logger.logMessage("Successfully redownloaded blockchain from " + (endHeight - numBlocks) + " to " + endHeight);
        compareTraceFiles();
        debugTrace.resetLog();
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
