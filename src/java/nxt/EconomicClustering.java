package nxt;

/**
 * Economic Clustering concept (EC) solves the most critical flaw of "classical" Proof-of-Stake - the problem called
 * "Nothing-at-Stake".
 *
 * I ought to respect BCNext's wish and say that this concept is inspired by Economic Majority idea of Meni Rosenfeld
 * (http://en.wikipedia.org/wiki/User:Meni_Rosenfeld).
 *
 * EC is a vital part of Transparent Forging. Words "Mining in Nxt relies on cooperation of people and even forces it"
 * (https://bitcointalk.org/index.php?topic=553205.0) were said about EC.
 *
 * Keep in mind that this concept has not been peer reviewed. You are very welcome to do it...
 *
 *                                                                              Come-from-Beyond (21.05.2014)
 */
final class EconomicClustering {

    private static final Blockchain blockchain = BlockchainImpl.getInstance();

    static Block getClusterDefiningBlockId(int timestamp) {
        Block block = blockchain.getLastBlock();
        int distance = 0;
        while (block.getTimestamp() > timestamp - Constants.EC_RULE_TERMINATOR && distance < Constants.EC_CLUSTER_BLOCK_DISTANCE_LIMIT) {
            block = blockchain.getBlock(block.getPreviousBlockId());
            distance += 1;
        }
        return block;
    }

    static boolean verifyFork(Transaction transaction) {
        if (blockchain.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8) {
            return true;
        }
        if (blockchain.getHeight() - transaction.getClusterDefiningBlockHeight() > Constants.EC_CLUSTER_BLOCK_DISTANCE_LIMIT) {
            return false;
        }
        Block clusterDefiningBlock = blockchain.getBlock(transaction.getClusterDefiningBlockId());
        return clusterDefiningBlock != null && clusterDefiningBlock.getHeight() == transaction.getClusterDefiningBlockHeight();
    }

}
