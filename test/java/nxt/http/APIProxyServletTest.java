package nxt.http;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class APIProxyServletTest {

    @Test
    public void passwordFinder() {
        ByteBuffer postData = ByteBuffer.wrap("abcsecretPhrase=def".getBytes());
        Assert.assertEquals(3, APIProxyServlet.PasswordFinder.process(postData, new String[] { "secretPhrase=" }));
        postData.rewind();
        Assert.assertEquals(-1, APIProxyServlet.PasswordFinder.process(postData, new String[] { "mySecret=" }));
        postData.rewind();
        Assert.assertEquals(3, APIProxyServlet.PasswordFinder.process(postData, new String[] { "mySecret=", "secretPhrase=" }));
        postData.rewind();
        Assert.assertEquals(0, APIProxyServlet.PasswordFinder.process(postData, new String[] { "secretPhrase=", "abc" }));
        postData.rewind();
        Assert.assertEquals(16, APIProxyServlet.PasswordFinder.process(postData, new String[] { "def" }));
    }

}
