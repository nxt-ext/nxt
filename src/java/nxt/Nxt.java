package nxt;

import nxt.http.API;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;

public final class Nxt {

    public static final String VERSION = "0.8.0";

    public static final int BLOCK_HEADER_LENGTH = 224;
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 128;
    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;

    public static final int ALIAS_SYSTEM_BLOCK = 22000;
    public static final int TRANSPARENT_FORGING_BLOCK = 30000;
    public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
    public static final int TRANSPARENT_FORGING_BLOCK_3 = 51000;
    public static final int TRANSPARENT_FORGING_BLOCK_4 = 64000;
    public static final int TRANSPARENT_FORGING_BLOCK_5 = 67000;
    public static final int ASSET_EXCHANGE_BLOCK = 111111;
    public static final int VOTING_SYSTEM_BLOCK = 222222;

    public static final long MAX_BALANCE = 1000000000;
    public static final long initialBaseTarget = 153722867;
    public static final long maxBaseTarget = MAX_BALANCE * initialBaseTarget;
    public static final long MAX_ASSET_QUANTITY = 1000000000;
    public static final int ASSET_ISSUANCE_FEE = 1000;
    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;
    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;

    public static final long epochBeginning;
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.ZONE_OFFSET, 0);
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        epochBeginning = calendar.getTimeInMillis();
    }

    public static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";

    private static final Properties properties = new Properties();

    public static int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined, default is " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringProperty(String name, String defaultValue) {
        String value = properties.getProperty(name);
        if (value != null) {
            Logger.logMessage(name + " = \"" + value.trim() + "\"");
            return value.trim();
        } else {
            Logger.logMessage(name + " not defined, default is " + defaultValue);
            return defaultValue;
        }
    }

    public static Boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, default is " + defaultValue);
        return defaultValue;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static void main(String[] args) {
        init();
        if (args.length == 1 && "reset".equalsIgnoreCase(args[0])) {
            getBlockchainProcessor().fullReset();
        }
    }

    public static void init() {
        Init.init();
    }

    private static void shutdown() {
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logMessage("Nxt server " + Nxt.VERSION + " stopped.");
    }

    private static class Init {

        static {

            System.out.println("Initializing Nxt server version " + Nxt.VERSION);

            long startTime = System.currentTimeMillis();
            try (InputStream is = ClassLoader.getSystemResourceAsStream("nxt.properties")) {
                Nxt.properties.load(is);
            } catch (IOException e) {
                throw new RuntimeException(e.toString(), e);
            }

            Logger.logMessage("logging enabled");

            if (! Nxt.getBooleanProperty("nxt.debugJetty", false)) {
                System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
                Logger.logDebugMessage("jetty logging disabled");
            }

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Nxt.shutdown();
                }
            }));

            Db.init();
            BlockchainProcessorImpl.getInstance();
            TransactionProcessorImpl.getInstance();
            Peers.init();
            Generator.init();
            API.init();
            Users.init();

            long currentTime = System.currentTimeMillis();
            Logger.logDebugMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
            Logger.logMessage("Nxt server " + Nxt.VERSION + " started successfully.");
        }

        private static void init() {}

        private Init() {} // never

    }

    private Nxt() {} // never

}
