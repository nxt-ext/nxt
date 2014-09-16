package nxt;


import nxt.db.Db;
import nxt.util.Convert;
import nxt.util.Listener;
import org.h2.tools.Shell;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

public class Helper {


    public static void generateBlock(String secretPhrase) {
        try {
            Nxt.getBlockchainProcessor().generateBlock(secretPhrase, Convert.getEpochTime());
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public static void executeQuery(String line) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        out.println(line);
        try {
            Shell shell = new Shell();
            shell.setErr(out);
            shell.setOut(out);
            shell.runTool(Db.getConnection(), "-sql", line);
        } catch (SQLException e) {
            out.println(e.toString());
        }
        System.out.println(new String(baos.toByteArray()));
    }

    public static class BlockListener implements Listener<Block> {
        @Override
        public void notify(Block block) {
            System.out.printf("Block Generated at height %d with %d transactions\n", block.getHeight(), block.getTransactionIds().size());
        }
    }
}
