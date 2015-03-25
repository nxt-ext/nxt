package nxt.http;

import java.util.Map;
import java.util.Set;
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
 *   id    - Thread identifier (defaults to all threads)
 *
 * Response parameters:
 *   threads - An array of thread trace objects
 *
 * Thread trace object:
 *   id    - The thread identifier
 *   name  - The thread name
 *   trace - An array of trace elements
 */
public class GetStackTraces extends APIServlet.APIRequestHandler {

    /** GetLog instance */
    static final GetStackTraces instance = new GetStackTraces();

    /**
     * Create the GetStackTraces instance
     */
    private GetStackTraces() {
        super(new APITag[] {APITag.DEBUG}, "id", "depth");
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
        // Get the thread identifier.  All threads will be dumped if
        // no identifier is specified.
        //
        long threadId;
        value  = req.getParameter("id");
        if (value != null)
            threadId = Long.valueOf(value);
        else
            threadId = -1;
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
        // Get the stack traces
        //
        JSONArray threadsJSON = new JSONArray();
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        if (stackTraces != null) {
            Set<Map.Entry<Thread, StackTraceElement[]>> traceSet = stackTraces.entrySet();
            traceSet.stream()
                    .filter((entry) -> threadId==-1 || entry.getKey().getId()==threadId)
                    .forEach((entry) -> {
                JSONObject threadJSON = new JSONObject();
                Thread thread = entry.getKey();
                StackTraceElement[] elements = entry.getValue();
                threadJSON.put("id", thread.getId());
                threadJSON.put("name", thread.getName());
                JSONArray traceJSON = new JSONArray();
                int ix = 0;
                for (StackTraceElement element : elements) {
                    traceJSON.add(element.toString());
                    if (++ix == depth)
                        break;
                }
                threadJSON.put("trace", traceJSON);
                threadsJSON.add(threadJSON);
            });
        }
        //
        // Return the response
        //
        JSONObject response = new JSONObject();
        response.put("threads", threadsJSON);
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
