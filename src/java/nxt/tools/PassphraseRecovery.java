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

package nxt.tools;

import nxt.Account;
import nxt.Constants;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.env.RuntimeEnvironment;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public final class PassphraseRecovery {

    private final Map<Long, byte[]> publicKeys = new HashMap<>();
    private Integer[] positions;
    private char[] wildcard;
    private char[] dictionary;

    public static void main(String[] args) {
        new PassphraseRecovery().recover();
    }

    private void recover() {
        try {
            String secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
            byte[] publicKey2 = Crypto.getPublicKey(secretPhrase);
            long id = Account.getId(publicKey2);
            String dbLocation = "./" + (Constants.isTestnet ? "nxt_test_db" : "nxt_db");
            dbLocation = RuntimeEnvironment.getDirProvider().getDbDir(dbLocation);
            try (Connection con = DriverManager.getConnection("jdbc:h2:" + dbLocation + "/nxt;DB_CLOSE_ON_EXIT=FALSE;MVCC=TRUE", "sa", "sa");
                 PreparedStatement selectBlocks = con.prepareStatement("SELECT * FROM public_key");
                 ResultSet rs = selectBlocks.executeQuery()) {
                while (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    byte[] publicKey = rs.getBytes("public_key");
                    publicKeys.put(accountId, publicKey);
                }
            }

            Logger.logMessage(String.format("Loaded %d public keys", publicKeys.size()));
            String wildcardStr = Nxt.getStringProperty("recoveryWildcard");
            StringBuilder sb = new StringBuilder();
            Set<Integer> wildcardPositions = new TreeSet<>();
            int pos = 0;
            wildcard = new char[wildcardStr.length()];
            for (int i = 0; i < wildcardStr.length(); i++) {
                char c = wildcardStr.charAt(i);
                if (c == '\\' && i < wildcardStr.length() - 1 && wildcardStr.charAt(i + 1) == '*') {
                    wildcard[pos] = '*';
                    i++;
                    pos++;
                    continue;
                }
                if (c == '*') {
                    wildcardPositions.add(pos);
                }
                wildcard[pos] = c;
                pos ++;
            }
            if (wildcard.length > pos) {
                System.arraycopy(wildcard, 0, wildcard, 0, pos);
            }
            String dictionaryStr = Nxt.getStringProperty("recoveryDictionary");
            if (dictionaryStr != null) {
                dictionary = dictionaryStr.toCharArray();
            } else {
                dictionary = new char[127 - 32 + 1];
                for (int i = 0; i < dictionary.length; i++) {
                    dictionary[i] = (char)(i + 32);
                }
            }

            Logger.logMessage(String.format("Wildcard %s positions %s dictionary %s", wildcardStr, wildcardPositions, Arrays.toString(dictionary)));
            positions = wildcardPositions.toArray(new Integer[wildcardPositions.size()]);
            scan(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scan(int pos) {
        for (char c : dictionary) {
            wildcard[positions[pos]] = c;
            if (pos < positions.length - 1) {
                scan(pos + 1);
            } else {
                String secretPhrase = new String(wildcard);
                byte[] publicKey = Crypto.getPublicKey(secretPhrase);
                long id = Account.getId(publicKey);
                if (publicKeys.get(id) != null) {
                    Logger.logMessage("Found passphrase " + secretPhrase);
                    return;
                }
            }
        }
    }

}
