/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public final class JSON {

    private JSON() {} //never

    public final static JSONStreamAware emptyJSON = prepare(new JSONObject());

    public static JSONStreamAware prepare(final JSONObject json) {
        return new JSONStreamAware() {
            private final char[] jsonChars = json.toJSONString().toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };
    }

    public static JSONStreamAware prepareRequest(final JSONObject json) {
        json.put("protocol", 1);
        return prepare(json);
    }

    public static String toString(JSONStreamAware jsonStreamAware) {
        StringWriter stringWriter = new StringWriter();
        try {
            jsonStreamAware.writeJSONString(stringWriter);
        } catch (IOException ignore) {}
        return stringWriter.toString();
    }
}
