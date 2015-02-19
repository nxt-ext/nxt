package nxt;

import nxt.env.DirProvider;
import nxt.env.RuntimeEnvironment;
import nxt.env.RuntimeMode;
import nxt.http.API;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.util.Time;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Nxt {

    public static final String VERSION = "1.5.0e";
    public static final String APPLICATION = "NRS";

    private static volatile Time time = new Time.EpochTime();

    public static final String NXT_DEFAULT_PROPERTIES = "nxt-default.properties";
    public static final String NXT_PROPERTIES = "nxt.properties";
    public static final String CONFIG_DIR = "conf";

    private static final RuntimeMode runtimeMode;
    private static final DirProvider dirProvider;

    private static final Properties defaultProperties = new Properties();
    static {
        System.out.println("Initializing Nxt server version " + Nxt.VERSION);
        printCommandLineArguments();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        dirProvider = RuntimeEnvironment.getDirProvider();
        loadProperties(defaultProperties, NXT_DEFAULT_PROPERTIES, true);
        if (!VERSION.equals(Nxt.defaultProperties.getProperty("nxt.version"))) {
            throw new RuntimeException("Using an nxt-default.properties file from a version other than " + VERSION + " is not supported!!!");
        }
    }

    private static final Properties properties = new Properties(defaultProperties);

    static {
        loadProperties(properties, NXT_PROPERTIES, false);
    }

    public static Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault) {
        try {
            // Load properties from location specified as command line parameter
            String configFile = System.getProperty(propertiesFile);
            if (configFile != null) {
                System.out.printf("Loading %s from %s\n", propertiesFile, configFile);
                try (InputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Error loading %s from %s", propertiesFile, configFile));
                }
            } else {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
                    // When running nxt.exe from a Windows installation we always have nxt.properties in the classpath but this is not the nxt properties file
                    // Therefore we first load it from the classpath and then look for the real nxt.properties in the user folder.
                    if (is != null) {
                        System.out.printf("Loading %s from classpath\n", propertiesFile);
                        properties.load(is);
                        if (isDefault) {
                            return properties;
                        }
                    }
                    // load non-default properties files from the user folder
                    if (!dirProvider.isLoadPropertyFileFromUserDir()) {
                        return properties;
                    }
                    if (!Files.isReadable(Paths.get(dirProvider.getUserHomeDir()))) {
                        System.out.printf("Creating dir %s\n", dirProvider.getUserHomeDir());
                        Files.createDirectory(Paths.get(dirProvider.getUserHomeDir()));
                    }
                    Path confDir = Paths.get(dirProvider.getUserHomeDir(), CONFIG_DIR);
                    if (!Files.isReadable(confDir)) {
                        System.out.printf("Creating dir %s\n", confDir);
                        Files.createDirectory(confDir);
                    }
                    Path propPath = Paths.get(confDir.toString(), propertiesFile);
                    if (Files.isReadable(propPath)) {
                        System.out.printf("Loading %s from dir %s\n", propertiesFile, confDir);
                        properties.load(Files.newInputStream(propPath));
                    } else {
                        System.out.printf("Creating property file %s\n", propPath);
                        Files.createFile(propPath);
                        Files.write(propPath, Convert.toBytes("# use this file for workstation specific " + propertiesFile));
                    }
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error loading " + propertiesFile, e);
                }
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace(); // make sure we log this exception
            throw e;
        }
    }

    private static void printCommandLineArguments() {
        try {
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (inputArguments != null && inputArguments.size() > 0) {
                System.out.println("Command line arguments");
            } else {
                return;
            }
            for (String inputArgument : inputArguments) {
                System.out.println(inputArgument);
            }
        } catch (AccessControlException e) {
            System.out.println("Cannot read input arguments " + e.getMessage());
        }
    }

    public static int getIntProperty(String name) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined, assuming 0");
            return 0;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined");
            return defaultValue;
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
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

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment)attachment);
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Nxt.time = time;
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Nxt.shutdown();
                }
            }));
            init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
        }
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        Logger.logShutdownMessage("Shutting down...");
        API.shutdown();
        Users.shutdown();
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("Nxt server " + VERSION + " stopped.");
        Logger.shutdown();
        runtimeMode.shutdown();
    }

    private static class Init {

        private static volatile boolean initialized = false;

        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                logSystemProperties();
                runtimeMode.init();
                setServerStatus("NXT Server - Loading database", null);
                Db.init();
                setServerStatus("NXT Server - Loading resources", null);
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                Alias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                PhasingPoll.init();
                Trade.init();
                AssetTransfer.init();
                Vote.init();
                PhasingVote.init();
                Currency.init();
                CurrencyBuyOffer.init();
                CurrencySellOffer.init();
                CurrencyFounder.init();
                CurrencyMint.init();
                CurrencyTransfer.init();
                Exchange.init();
                Peers.init();
                Generator.init();
                API.init();
                Users.init();
                DebugTrace.init();
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Nxt.getIntProperty("nxt.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Nxt.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }

                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage("Nxt server " + VERSION + " started successfully.");
                setServerStatus("NXT Server - Online", API.getBrowserUri());
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                System.exit(1);
            }
        }

        private static void init() {
            if (initialized) {
                throw new RuntimeException("Nxt.init has already been called");
            }
            initialized = true;
        }

        private Init() {} // never

    }

    private static void logSystemProperties() {
        String[] loggedProperties = new String[] {
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding",
                RuntimeMode.RUNTIME_MODE_ARG
        };
        for (String property : loggedProperties) {
            Logger.logDebugMessage(String.format("%s = %s", property, System.getProperty(property)));
        }
        Logger.logDebugMessage(String.format("availableProcessors = %s", Runtime.getRuntime().availableProcessors()));
        Logger.logDebugMessage(String.format("maxMemory = %s", Runtime.getRuntime().maxMemory()));
        Logger.logDebugMessage(String.format("processId = %s", getProcessId()));
    }

    public static String getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (runtimeName == null) {
            return "";
        }
        String[] tokens = runtimeName.split("@");
        if (tokens.length == 2) {
            return tokens[0];
        }
        return "";
    }

    public static String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir);
    }

    public static void updateLogFileHandler(Properties loggingProperties) {
        dirProvider.updateLogFileHandler(loggingProperties);
    }

    public static String getUserHomeDir() {
        return dirProvider.getUserHomeDir();
    }

    public static void setServerStatus(String status, URI wallet) {
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogFileDir());
    }

    private Nxt() {} // never

}
