package nxt.util;

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

    private Logger() {} //never

    public static void logMessage(String message) {
        System.out.println(logDateFormat.get().format(new Date()) + message);
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
