package nxt.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;

public final class JSON {

    private JSON() {} //never

    public final static JSONStreamAware emptyJSON = getJSONStreamAware(new JSONObject());

    public static JSONStreamAware getJSONStreamAware(final JSONObject json) {
        return new JSONStreamAware() {
            private final char[] jsonChars = json.toJSONString().toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };
    }

}
