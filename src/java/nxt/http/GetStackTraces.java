package nxt.http;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * The GetStackTraces API will return the current stack trace for
 * each Nxt thread.
 *
 * Request parameters:
 *   depth - Stack trace depth (minimum 1, defaults to full trace)
 *
 * Response parameters:
 *   locks   - An array of lock objects for locks with waiters
 *   threads - An array of thread objects
 *
 * Lock object:
 *   name   - Lock class name
 *   hash   - Lock identity hash code
 *   thread - Identifier of thread holding the lock
 *
 * Thread object:
 *   blocked - Lock object if thread is waiting on a lock
 *   id      - Thread identifier
 *   locks   - Array of lock objects for locks held by this thread
 *   name    - Thread name
 *   state   - Thread state
 *   trace   - Array of stack trace elements
 */
public class GetStackTraces extends APIServlet.APIRequestHandler {

    /** GetLog instance */
    static final GetStackTraces instance = new GetStackTraces();

    /**
     * Create the GetStackTraces instance
     */
    private GetStackTraces() {
        super(new APITag[] {APITag.DEBUG}, "depth");
    }

    /**
     * Process the GetStackTraces API request
     *
     * @param   req                 API request
     * @return                      API response
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        String value;
        //
        // Get the number of trace lines to return
        //
        int depth;
        value = req.getParameter("depth");
        if (value != null)
            depth = Math.max(Integer.valueOf(value), 1);
        else
            depth = Integer.MAX_VALUE;
        //
        // Get the thread information
        //
        JSONArray threadsJSON = new JSONArray();
        JSONArray locksJSON = new JSONArray();
        ThreadMXBean tmxBean = ManagementFactory.getThreadMXBean();
        boolean tmxMI = tmxBean.isObjectMonitorUsageSupported();
        ThreadInfo[] tList = tmxBean.dumpAllThreads(tmxMI, false);
        //
        // Generate the response
        //
        for (ThreadInfo tInfo : tList) {
            JSONObject threadJSON = new JSONObject();
            //
            // General thread information
            //
            threadJSON.put("id", tInfo.getThreadId());
            threadJSON.put("name", tInfo.getThreadName());
            threadJSON.put("state", tInfo.getThreadState().toString());
            //
            // Gather lock usage
            //
            if (tmxMI) {
                MonitorInfo[] mList = tInfo.getLockedMonitors();
                if (mList.length > 0) {
                    JSONArray monitorsJSON = new JSONArray();
                    for (MonitorInfo mInfo : mList) {
                        JSONObject lockJSON = new JSONObject();
                        lockJSON.put("name", mInfo.getClassName());
                        lockJSON.put("hash", mInfo.getIdentityHashCode());
                        lockJSON.put("thread", tInfo.getThreadId());
                        monitorsJSON.add(lockJSON);
                    }
                    threadJSON.put("locks", monitorsJSON);
                }
                if (tInfo.getThreadState() == Thread.State.BLOCKED) {
                    LockInfo lInfo = tInfo.getLockInfo();
                    if (lInfo != null) {
                        JSONObject lockJSON = new JSONObject();
                        lockJSON.put("name", lInfo.getClassName());
                        lockJSON.put("hash", lInfo.getIdentityHashCode());
                        lockJSON.put("thread", tInfo.getLockOwnerId());
                        threadJSON.put("blocked", lockJSON);
                        boolean addLock = true;
                        for (Object lock : locksJSON){
                            if (((String)((JSONObject)lock).get("name")).equals(lInfo.getClassName())) {
                                addLock = false;
                                break;
                            }
                        }
                        if (addLock)
                            locksJSON.add(lockJSON);
                    }
                }
            }
            //
            // Add the stack trace
            //
            StackTraceElement[] elements = tInfo.getStackTrace();
            JSONArray traceJSON = new JSONArray();
            int ix = 0;
            for (StackTraceElement element : elements) {
                traceJSON.add(element.toString());
                if (++ix == depth)
                    break;
            }
            threadJSON.put("trace", traceJSON);
            //
            // Add the thread to the response
            //
            threadsJSON.add(threadJSON);
        }
        //
        // Return the response
        //
        JSONObject response = new JSONObject();
        response.put("threads", threadsJSON);
        response.put("locks", locksJSON);
        return response;
    }

    /**
     * Require the administrator password
     *
     * @return                      TRUE if the admin password is required
     */
    @Override
    boolean requirePassword() {
        return true;
    }
}
