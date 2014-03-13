package nxt;

import nxt.util.DbIterator;

import java.util.List;

public interface Blockchain {

    Block getLastBlock();

    Block getBlock(Long blockId);

    boolean hasBlock(Long blockId);

    int getBlockCount();

    DbIterator<? extends Block> getAllBlocks();

    DbIterator<? extends Block> getAllBlocks(Account account, int timestamp);

    List<Long> getBlockIdsAfter(Long blockId, int limit);

    List<? extends Block> getBlocksAfter(Long blockId, int limit);

    long getBlockIdAtHeight(int height);

    List<? extends Block> getBlocksFromHeight(int height);

    Transaction getTransaction(Long transactionId);

    Transaction getTransaction(String hash);

    boolean hasTransaction(Long transactionId);

    int getTransactionCount();

    DbIterator<? extends Transaction> getAllTransactions();

    DbIterator<? extends Transaction> getAllTransactions(Account account, byte type, byte subtype, int timestamp);

    DbIterator<? extends Transaction> getAllTransactions(Account account, byte type, byte subtype, int timestamp, Boolean orderAscending);

}
