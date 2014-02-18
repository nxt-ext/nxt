package nxt.util;

import nxt.Nxt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger {

    private static final boolean debug;
    private static final boolean enableStackTraces;

    private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
        }
    };

    private static PrintWriter fileLog = null;
    static {
        debug = Nxt.getBooleanProperty("nxt.debug", false);
        enableStackTraces = Nxt.getBooleanProperty("nxt.enableStackTraces", true);
        try {
            fileLog = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Nxt.getStringProperty("nxt.log", "nxt.log"))))), true);
        } catch (IOException e) {
            logMessage("Logging to file nxt.log not possible, will log to stdout only");
        }
        logMessage("Debug logging " + (debug ? "enabled" : "disabled"));
        logMessage("Exception stack traces " + (enableStackTraces ? "enabled" : "disabled"));
    }

    private Logger() {} //never

    public static void logMessage(String message) {
        String logEntry = logDateFormat.get().format(new Date()) + message;
        System.out.println(logEntry);
        if (fileLog != null) {
            fileLog.println(logEntry);
        }
    }

    public static void logMessage(String message, Exception e) {
        if (enableStackTraces) {
            logMessage(message);
            e.printStackTrace();
        } else {
            logMessage(message + ":\n" + e.toString());
        }
    }

    public static void logDebugMessage(String message) {
        if (debug) {
            logMessage("DEBUG: " + message);
        }
    }

    public static void logDebugMessage(String message, Exception e) {
        if (enableStackTraces) {
            logMessage("DEBUG: " + message);
            e.printStackTrace();
        } else if (debug) {
            logMessage("DEBUG: " + message + ":\n" + e.toString());
        }
    }
}
