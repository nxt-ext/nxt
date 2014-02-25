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

    public static enum Event {
        MESSAGE, EXCEPTION
    }

    private static final boolean debug;
    private static final boolean enableStackTraces;

    private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
        }
    };

    private static final Listeners<String,Event> messageListeners = new Listeners<>();
    private static final Listeners<Exception,Event> exceptionListeners = new Listeners<>();

    private static final PrintWriter fileLog;
    static {
        debug = Nxt.getBooleanProperty("nxt.debug");
        enableStackTraces = Nxt.getBooleanProperty("nxt.enableStackTraces");
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Nxt.getStringProperty("nxt.log"))))), true);
        } catch (IOException|RuntimeException e) {
            logMessage("Logging to file not possible, will log to stdout only", e);
        }
        fileLog = printWriter;
        logMessage("Debug logging " + (debug ? "enabled" : "disabled"));
        logMessage("Exception stack traces " + (enableStackTraces ? "enabled" : "disabled"));
    }

    private Logger() {} //never

    public static boolean addMessageListener(Listener<String> listener, Event eventType) {
        return messageListeners.addListener(listener, eventType);
    }

    public static boolean addExceptionListener(Listener<Exception> listener, Event eventType) {
        return exceptionListeners.addListener(listener, eventType);
    }

    public static boolean removeMessageListener(Listener<String> listener, Event eventType) {
        return messageListeners.removeListener(listener, eventType);
    }

    public static boolean removeExceptionListener(Listener<Exception> listener, Event eventType) {
        return exceptionListeners.removeListener(listener, eventType);
    }

    public static void logMessage(String message) {
        String logEntry = logDateFormat.get().format(new Date()) + message;
        System.out.println(logEntry);
        if (fileLog != null) {
            fileLog.println(logEntry);
        }
        messageListeners.notify(message, Event.MESSAGE);
    }

    public static void logMessage(String message, Exception e) {
        if (enableStackTraces) {
            logMessage(message);
            e.printStackTrace(System.out);
            if (fileLog != null) {
                e.printStackTrace(fileLog);
            }
        } else {
            logMessage(message + ":\n" + e.toString());
        }
        exceptionListeners.notify(e, Event.EXCEPTION);
    }

    public static void logDebugMessage(String message) {
        if (debug) {
            logMessage("DEBUG: " + message);
        }
    }

    public static void logDebugMessage(String message, Exception e) {
        if (enableStackTraces) {
            logMessage("DEBUG: " + message);
            e.printStackTrace(System.out);
            if (fileLog != null) {
                e.printStackTrace(fileLog);
            }
        } else if (debug) {
            logMessage("DEBUG: " + message + ":\n" + e.toString());
        }
        exceptionListeners.notify(e, Event.EXCEPTION);
    }
}
