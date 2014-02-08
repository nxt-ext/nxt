package nxt.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger {

    private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
        }
    };

    public static final boolean debug = System.getProperty("nxt.debug") != null;
    public static final boolean enableStackTraces = System.getProperty("nxt.enableStackTraces") != null;

    private static PrintWriter fileLog = null;
    static {
        try {
            fileLog = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream("nxt.log")))), true);
        } catch (IOException e) {
            System.out.println("Logging to file nxt.log not possible, will log to stdout only");
        }
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
        if (debug) {
            if (enableStackTraces) {
                logMessage("DEBUG: " + message);
                e.printStackTrace();
            } else {
                logMessage("DEBUG: " + message + ":\n" + e.toString());
            }
        }
    }
}
