package nxt.http;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class APIProxyServletTest {

    @Test
    public void passwordFinder() {
        APIProxyServlet.PasswordFinder passwordFinder = new APIProxyServlet.PasswordFinder("secretPhrase=");
        Assert.assertTrue(passwordFinder.process(ByteBuffer.wrap("abcsecretPhrase=def".getBytes())));
        passwordFinder = new APIProxyServlet.PasswordFinder("secretPhrase=");
        Assert.assertFalse(passwordFinder.process(ByteBuffer.wrap("abcsecretPhrasedef".getBytes())));
    }

}
