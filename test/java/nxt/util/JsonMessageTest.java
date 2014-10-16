package nxt.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class JsonMessageTest {

    @Test
    public void message() {
        String message = "{\n  \"type\": \"dividend\",\n  \"contractId\": \"2112610727280991058\",\n  \"height\": 260315,\n  \"total\": \"42700000000\",\n  \"percentage\": \"0%\",\n  \"shares\": 50\n}";
        JSONObject request = new JSONObject();
        JSONObject response = new JSONObject();
        request.put("message", message);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            try (Writer writer = new OutputStreamWriter(byteArrayOutputStream, "UTF-8")) {
                request.writeJSONString(writer);
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            try (Reader reader = new BufferedReader(new InputStreamReader(byteArrayInputStream, "UTF-8"))) {
                response = (JSONObject)JSONValue.parse(reader);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Assert.assertEquals(message, response.get("message"));
    }

}
