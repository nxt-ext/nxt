/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2018 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.http.accountproperties;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.http.APICall;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class AccountInfoTest extends BlockchainTest {
    @Test
    public void testNameLen160() {
        char[] fourBytesChar = Character.toChars(0x1F701);
        String specialChar = new String(fourBytesChar);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 40; i++) {
            sb.append(specialChar);
        }
        String name = sb.toString();
        APICall.Builder builder = new APICall.Builder("setAccountInfo").
                param("secretPhrase", ALICE.getSecretPhrase()).feeNQT(Constants.ONE_NXT * 20).
                param("name", name);
        JSONObject response = builder.build().invoke();
        Assert.assertEquals(4L, response.get("errorCode"));
        Assert.assertTrue(((String)response.get("errorDescription")).contains("Invalid account info issuance"));
        BlockchainTest.generateBlock();

        String fixedName = name.substring(0, 40); //the specialChar is 2 characters long
        String description = name + name;
        builder.param("name", fixedName).param("description", description);
        response = builder.build().invoke();
        Assert.assertNull(response.get("errorCode"));
        BlockchainTest.generateBlock();

        response = new APICall.Builder("getAccount").param("account", ALICE.getRsAccount()).build().invoke();
        Assert.assertEquals(fixedName, response.get("name"));
        Assert.assertEquals(description, response.get("description"));
    }

    @Test
    public void testNameLen258() {
        String char3Byte = "€";
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 86; i++) {
            sb.append(char3Byte);
        }
        String name = sb.toString();
        APICall.Builder builder = new APICall.Builder("setAccountInfo").
                param("secretPhrase", ALICE.getSecretPhrase()).feeNQT(Constants.ONE_NXT * 20).
                param("name", name);
        JSONObject response = builder.build().invoke();
        Assert.assertEquals(4L, response.get("errorCode"));
        Assert.assertTrue(((String)response.get("errorDescription")).contains("Invalid account info issuance"));
        BlockchainTest.generateBlock();

        String fixedName = name.substring(0, 33);
        response = builder.param("name", fixedName).build().invoke();
        Assert.assertNull(response.get("errorCode"));
        BlockchainTest.generateBlock();

        response = new APICall.Builder("getAccount").param("account", ALICE.getRsAccount()).build().invoke();
        Assert.assertEquals(fixedName, response.get("name"));
    }
}
