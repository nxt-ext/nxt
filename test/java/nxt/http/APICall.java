package nxt.http;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class APICall {

    private Map<String, String> params = new HashMap<>();

    private APICall(Builder builder) {
        this.params = builder.params;
    }

    public static class Builder {

        protected Map<String, String> params = new HashMap<>();

        public Builder(String requestType) {
            params.put("requestType", requestType);
            params.put("deadline", "1440");
        }

        public Builder param(String key, String value) {
            params.put(key, value);
            return this;
        }

        public Builder param(String key, byte value) {
            params.put(key, "" + value);
            return this;
        }

        public Builder param(String key, int value) {
            params.put(key, "" + value);
            return this;
        }

        public Builder param(String key, long value) {
            params.put(key, "" + value);
            return this;
        }

        public Builder secretPhrase(String value) {
            params.put("secretPhrase", value);
            return this;
        }

        public Builder feeNQT(long value) {
            params.put("feeNQT", "" + value);
            return this;
        }

        public APICall build() {
            return new APICall(this);
        }
    }

    public JSONObject invoke() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getRemoteHost()).thenReturn("localhost");
        when(req.getMethod()).thenReturn("POST");
        for (String key : params.keySet()) {
            when(req.getParameter(key)).thenReturn(params.get(key));
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
        return (JSONObject)JSONValue.parse(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
    }

}
