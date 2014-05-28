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

    static Block getClusterDefiningBlockId(int timestamp) {
        Block block = BlockchainImpl.getInstance().getLastBlock();
        do {
            block = BlockchainImpl.getInstance().getBlock(block.getPreviousBlockId());
        } while (block.getTimestamp() > timestamp - Constants.RULE_TERMINATOR);
        return block;
    }

    static boolean validateClusterDefiningBlock(int height, Long id) {

        return BlockchainImpl.getInstance().getBlock(BlockchainImpl.getInstance().getBlockIdAtHeight(height)).getId().equals(id);

    }

}
