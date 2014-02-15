package nxt.user;

import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import static nxt.user.JSONResponses.LOCK_ACCOUNT;

final class UnlockAccount extends UserRequestHandler {

    static final UnlockAccount instance = new UnlockAccount();

    private UnlockAccount() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
        String secretPhrase = req.getParameter("secretPhrase");
        // lock all other instances of this account being unlocked
        for (User u : User.getAllUsers()) {
            if (secretPhrase.equals(u.getSecretPhrase())) {
                u.lockAccount();
                if (! u.isInactive()) {
                    u.enqueue(LOCK_ACCOUNT);
                }
            }
        }

        Long accountId = user.unlockAccount(secretPhrase);

        JSONObject response = new JSONObject();
        response.put("response", "unlockAccount");
        response.put("account", Convert.convert(accountId));

        if (secretPhrase.length() < 30) {

            response.put("secretPhraseStrength", 1);

        } else {

            response.put("secretPhraseStrength", 5);

        }

        Account account = Account.getAccount(accountId);
        if (account == null) {

            response.put("balance", 0);

        } else {

            response.put("balance", account.getUnconfirmedBalance());

            JSONArray myTransactions = new JSONArray();
            byte[] accountPublicKey = account.getPublicKey();
            for (Transaction transaction : Blockchain.getAllUnconfirmedTransactions()) {

                if (Arrays.equals(transaction.getSenderPublicKey(), accountPublicKey)) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", User.getIndex(transaction));
                    myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    myTransaction.put("deadline", transaction.getDeadline());
                    myTransaction.put("account", Convert.convert(transaction.getRecipientId()));
                    myTransaction.put("sentAmount", transaction.getAmount());
                    if (accountId.equals(transaction.getRecipientId())) {

                        myTransaction.put("receivedAmount", transaction.getAmount());

                    }
                    myTransaction.put("fee", transaction.getFee());
                    myTransaction.put("numberOfConfirmations", 0);
                    myTransaction.put("id", transaction.getStringId());

                    myTransactions.add(myTransaction);

                } else if (accountId.equals(transaction.getRecipientId())) {

                    JSONObject myTransaction = new JSONObject();
                    myTransaction.put("index", User.getIndex(transaction));
                    myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                    myTransaction.put("deadline", transaction.getDeadline());
                    myTransaction.put("account", Convert.convert(transaction.getSenderId()));
                    myTransaction.put("receivedAmount", transaction.getAmount());
                    myTransaction.put("fee", transaction.getFee());
                    myTransaction.put("numberOfConfirmations", 0);
                    myTransaction.put("id", transaction.getStringId());

                    myTransactions.add(myTransaction);

                }

            }

            SortedMap<Integer,JSONObject> myTransactionsMap = new TreeMap<>();

            int blockchainHeight = Blockchain.getLastBlock().getHeight();
            try (DbIterator<Block> blockIterator = Blockchain.getAllBlocks(account, 0)) {
                while (blockIterator.hasNext()) {
                    Block block = blockIterator.next();
                    if (block.getTotalFee() > 0) {
                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", User.getIndex(block));
                        myTransaction.put("blockTimestamp", block.getTimestamp());
                        myTransaction.put("block", block.getStringId());
                        myTransaction.put("earnedAmount", block.getTotalFee());
                        myTransaction.put("numberOfConfirmations", blockchainHeight - block.getHeight());
                        myTransaction.put("id", "-");
                        myTransactionsMap.put(-block.getTimestamp(), myTransaction);
                    }
                }
            }

            try (DbIterator<Transaction> transactionIterator = Blockchain.getAllTransactions(account, (byte)-1, (byte)-1, 0, null)) {
                while (transactionIterator.hasNext()) {
                    Transaction transaction = transactionIterator.next();
                    if (transaction.getSenderId().equals(accountId)) {
                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", User.getIndex(transaction));
                        myTransaction.put("blockTimestamp", transaction.getBlock().getTimestamp());
                        myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                        myTransaction.put("account", Convert.convert(transaction.getRecipientId()));
                        myTransaction.put("sentAmount", transaction.getAmount());
                        if (accountId.equals(transaction.getRecipientId())) {
                            myTransaction.put("receivedAmount", transaction.getAmount());
                        }
                        myTransaction.put("fee", transaction.getFee());
                        myTransaction.put("numberOfConfirmations", blockchainHeight - transaction.getBlock().getHeight());
                        myTransaction.put("id", transaction.getStringId());
                        myTransactionsMap.put(-transaction.getTimestamp(), myTransaction);
                    } else if (transaction.getRecipientId().equals(accountId)) {
                        JSONObject myTransaction = new JSONObject();
                        myTransaction.put("index", User.getIndex(transaction));
                        myTransaction.put("blockTimestamp", transaction.getBlock().getTimestamp());
                        myTransaction.put("transactionTimestamp", transaction.getTimestamp());
                        myTransaction.put("account", Convert.convert(transaction.getSenderId()));
                        myTransaction.put("receivedAmount", transaction.getAmount());
                        myTransaction.put("fee", transaction.getFee());
                        myTransaction.put("numberOfConfirmations", blockchainHeight - transaction.getBlock().getHeight());
                        myTransaction.put("id", transaction.getStringId());
                        myTransactionsMap.put(-transaction.getTimestamp(), myTransaction);
                    }
                }
            }

            Iterator<JSONObject> iterator = myTransactionsMap.values().iterator();
            while (myTransactions.size() < 1000 && iterator.hasNext()) {
                myTransactions.add(iterator.next());
            }

            if (myTransactions.size() > 0) {
                JSONObject response2 = new JSONObject();
                response2.put("response", "processNewData");
                response2.put("addedMyTransactions", myTransactions);
                user.enqueue(response2);
            }
        }
        return response;
    }

}
