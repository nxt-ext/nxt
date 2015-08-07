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

package nxt.tools;

import nxt.Nxt;
import nxt.Transaction;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;

public final class SignTransactionJSON {

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("Usage: SignTransactionJSON <unsigned transaction json file> <signed transaction json file>");
                System.exit(1);
            }
            File unsigned = new File(args[0]);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                System.exit(1);
            }
            File signed = new File(args[1]);
            if (signed.exists()) {
                System.out.println("File already exists: " + signed.getAbsolutePath());
                System.exit(1);
            }
            String secretPhrase;
            Console console = System.console();
            if (console == null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    secretPhrase = reader.readLine();
                }
            } else {
                secretPhrase = new String(console.readPassword("Secret phrase: "));
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(unsigned));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(signed))) {
                JSONObject json = (JSONObject) JSONValue.parseWithException(reader);
                Transaction.Builder builder = Nxt.newTransactionBuilder(json);
                Transaction transaction = builder.build(secretPhrase);
                writer.write(transaction.getJSONObject().toJSONString());
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
