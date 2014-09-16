package nxt.http;

import org.json.simple.JSONValue;
import org.junit.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class APIHelper {


    static Object processRequest(Map<String, String> reqParams) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getRemoteHost()).thenReturn("localhost");
        when(req.getMethod()).thenReturn("POST");
        for (String key : reqParams.keySet()) {
            when(req.getParameter(key)).thenReturn(reqParams.get(key));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        try {
            when(resp.getWriter()).thenReturn(writer);
            APIServlet apiServlet = new APIServlet();
            apiServlet.doPost(req, resp);
        } catch (ServletException | IOException e) {
            Assert.fail();
        }
        return JSONValue.parse(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
    }
}
