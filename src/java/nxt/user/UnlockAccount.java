package nxt.user;

import nxt.Account;
import nxt.Block;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

final class UnlockAccount extends UserRequestHandler {

    static final UnlockAccount instance = new UnlockAccount();

    private UnlockAccount() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req, User user) throws IOException {
        String secretPhrase = req.getParameter("secretPhrase");
        // lock all other instances of this account being unlocked
        for (User u : Nxt.users.values()) {
            if (secretPhrase.equals(u.secretPhrase)) {
                u.deinitializeKeyPair();
                if (! u.isInactive) {
                    JSONObject response = new JSONObject();
                    response.put("response", "lockAccount");
                    u.pendingResponses.offer(response);
                }
            }
        }

        BigInteger bigInt = user.initializeKeyPair(secretPhrase);
        long accountId = bigInt.longValue();

        JSONObject response = new JSONObject();
        response.put("response", "unlockAccount");
        response.put("account", bigInt.toString());

        if (secretPhrase.length() < 30) {

            response.put("secretPhraseStrength", 1);

        } else {

            response.put("secretPhraseStrength", 5);

        }

        Account account = Nxt.accounts.get(accountId);
        if (account == null) {

            response.put("balance", 0);

        } else {

            response.put("balance", account.getUnconfirmedBalance());

            long effectiveBalance = account.getEffectiveBalance();
            if (effectiveBalance > 0) {

                JSONObject response2 = new JSONObject();
                response2.put("response", "setBlockGenerationDeadline");

                Block lastBlock = Nxt.lastBlock.get();
                MessageDigest digest = Crypto.sha256();
                byte[] generationSignatureHash;
                if (lastBlock.height < Nxt.TRANSPARENT_FORGING_BLOCK) {

                    byte[] generationSignature = Crypto.sign(lastBlock.generationSignature, user.secretPhrase);
                    generationSignatureHash = digest.digest(generationSignature);

                } else {

                    digest.update(lastBlock.generationSignature);
                    generationSignatureHash = digest.digest(user.publicKey);

                }
                BigInteger hit = new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
                response2.put("deadline", hit.divide(BigInteger.valueOf(lastBlock.baseTarget).multiply(BigInteger.valueOf(effectiveBalance))).longValue() - (Convert.getEpochTime() - lastBlock.timestamp));

                user.pendingResponses.offer(response2);

            }

            JSONArray myTransactions = new JSONArray();
            byte[] accountPublicKey = account.publicKey.get();
            for (Transaction transaction : Nxt.unconfirmedTransactions.values()) {

                if (Arrays.equals(transaction.senderPublicKey, accountPublicKey)) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", transaction.index);
                    myTransaction.put("transactionTimestamp", transaction.timestamp);
                    myTransaction.put("deadline", transaction.deadline);
                    myTransaction.put("account", Convert.convert(transaction.recipient));
                    myTransaction.put("sentAmount", transaction.amount);
                    if (transaction.recipient == accountId) {

                        myTransaction.put("receivedAmount", transaction.amount);

                    }
                    myTransaction.put("fee", transaction.fee);
                    myTransaction.put("numberOfConfirmations", 0);
                    myTransaction.put("id", transaction.getStringId());

                    myTransactions.add(myTransaction);

                } else if (transaction.recipient == accountId) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", transaction.index);
                    myTransaction.put("transactionTimestamp", transaction.timestamp);
                    myTransaction.put("deadline", transaction.deadline);
                    myTransaction.put("account", Convert.convert(transaction.getSenderAccountId()));
                    myTransaction.put("receivedAmount", transaction.amount);
                    myTransaction.put("fee", transaction.fee);
                    myTransaction.put("numberOfConfirmations", 0);
                    myTransaction.put("id", transaction.getStringId());

                    myTransactions.add(myTransaction);

                }

            }

            long blockId = Nxt.lastBlock.get().getId();
            int numberOfConfirmations = 1;
            while (myTransactions.size() < 1000) {

                Block block = Nxt.blocks.get(blockId);

                if (block.totalFee > 0 && Arrays.equals(block.generatorPublicKey, accountPublicKey)) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", block.getStringId()); // cfb: Generated fee transactions get an id equal to the block id
                    myTransaction.put("blockTimestamp", block.timestamp);
                    myTransaction.put("block", block.getStringId());
                    myTransaction.put("earnedAmount", block.totalFee);
                    myTransaction.put("numberOfConfirmations", numberOfConfirmations);
                    myTransaction.put("id", "-");

                    myTransactions.add(myTransaction);

                }

                for (Transaction transaction : block.blockTransactions) {

                    if (Arrays.equals(transaction.senderPublicKey, accountPublicKey)) {

                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", transaction.index);
                        myTransaction.put("blockTimestamp", block.timestamp);
                        myTransaction.put("transactionTimestamp", transaction.timestamp);
                        myTransaction.put("account", Convert.convert(transaction.recipient));
                        myTransaction.put("sentAmount", transaction.amount);
                        if (transaction.recipient == accountId) {

                            myTransaction.put("receivedAmount", transaction.amount);

                        }
                        myTransaction.put("fee", transaction.fee);
                        myTransaction.put("numberOfConfirmations", numberOfConfirmations);
                        myTransaction.put("id", transaction.getStringId());

                        myTransactions.add(myTransaction);

                    } else if (transaction.recipient == accountId) {

                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", transaction.index);
                        myTransaction.put("blockTimestamp", block.timestamp);
                        myTransaction.put("transactionTimestamp", transaction.timestamp);
                        myTransaction.put("account", Convert.convert(transaction.getSenderAccountId()));
                        myTransaction.put("receivedAmount", transaction.amount);
                        myTransaction.put("fee", transaction.fee);
                        myTransaction.put("numberOfConfirmations", numberOfConfirmations);
                        myTransaction.put("id", transaction.getStringId());

                        myTransactions.add(myTransaction);
                    }
                }
                if (blockId == Genesis.GENESIS_BLOCK_ID) {
                    break;
                }
                blockId = block.previousBlock;
                numberOfConfirmations++;
            }
            if (myTransactions.size() > 0) {
                JSONObject response2 = new JSONObject();
                response2.put("response", "processNewData");
                response2.put("addedMyTransactions", myTransactions);
                user.pendingResponses.offer(response2);
            }
        }
        return response;
    }

}
