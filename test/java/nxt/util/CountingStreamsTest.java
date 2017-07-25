/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class CountingStreamsTest {

    @Test
    public void countingInputReader() {
        String s = "abcdefgh";
        try (CountingInputReader countingInputReader = new CountingInputReader(new StringReader(s), Long.MAX_VALUE)) {
            while (countingInputReader.read() != -1) {}
            Assert.assertEquals(s.length(), countingInputReader.getCount());
        } catch (IOException ignore) {}
        try (CountingInputReader countingInputReader = new CountingInputReader(new StringReader(s), Long.MAX_VALUE)) {
            countingInputReader.read(new char[2]);
            Assert.assertEquals(2, countingInputReader.getCount());
        } catch (IOException ignore) {}
        try (CountingInputReader countingInputReader = new CountingInputReader(new StringReader(s), Long.MAX_VALUE)) {
            countingInputReader.read(new char[100]);
            Assert.assertEquals(s.length(), countingInputReader.getCount());
        } catch (IOException ignore) {}
        try (CountingInputReader countingInputReader = new CountingInputReader(new StringReader(s), Long.MAX_VALUE)) {
            countingInputReader.read(new char[100], 10, 5);
            Assert.assertEquals(5, countingInputReader.getCount());
        } catch (IOException ignore) {}
        try (CountingInputReader countingInputReader = new CountingInputReader(new StringReader(s), Long.MAX_VALUE)) {
            countingInputReader.skip(2);
            while (countingInputReader.read() > 0) {}
            Assert.assertEquals(s.length(), countingInputReader.getCount());
        } catch (IOException ignore) {}
    }

    @Test
    public void countingOutputWriter() {
        StringWriter stringWriter = new StringWriter();
        String s = "abcdefgh";
        try (CountingOutputWriter countingOutputWriter = new CountingOutputWriter(stringWriter)) {
            for (int i = 0; i < s.length(); i++) {
                countingOutputWriter.write(s.charAt(i));
            }
            Assert.assertEquals(s.length(), countingOutputWriter.getCount());
        } catch (IOException ignore) {}
        try (CountingOutputWriter countingOutputWriter = new CountingOutputWriter(stringWriter)) {
            countingOutputWriter.write(s.toCharArray());
            Assert.assertEquals(s.length(), countingOutputWriter.getCount());
        } catch (IOException ignore) {}
        try (CountingOutputWriter countingOutputWriter = new CountingOutputWriter(stringWriter)) {
            countingOutputWriter.write(s);
            Assert.assertEquals(s.length(), countingOutputWriter.getCount());
        } catch (IOException ignore) {}
        try (CountingOutputWriter countingOutputWriter = new CountingOutputWriter(stringWriter)) {
            countingOutputWriter.write(s.toCharArray(), 2, 3);
            Assert.assertEquals(3, countingOutputWriter.getCount());
        } catch (IOException ignore) {}
        try (CountingOutputWriter countingOutputWriter = new CountingOutputWriter(stringWriter)) {
            countingOutputWriter.write(s, 2, 3);
            Assert.assertEquals(3, countingOutputWriter.getCount());
        } catch (IOException ignore) {}
    }

}
