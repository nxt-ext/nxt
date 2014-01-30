package nxt.user;

import nxt.Block;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Nxt;
import nxt.Transaction;
import nxt.peer.Peer;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigInteger;

final class GetInitialData extends UserRequestHandler {

    static final GetInitialData instance = new GetInitialData();

    private GetInitialData() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {

        JSONArray unconfirmedTransactions = new JSONArray();
        JSONArray activePeers = new JSONArray(), knownPeers = new JSONArray(), blacklistedPeers = new JSONArray();
        JSONArray recentBlocks = new JSONArray();

        for (Transaction transaction : Blockchain.allUnconfirmedTransactions) {

            JSONObject unconfirmedTransaction = new JSONObject();
            unconfirmedTransaction.put("index", transaction.getIndex());
            unconfirmedTransaction.put("timestamp", transaction.getTimestamp());
            unconfirmedTransaction.put("deadline", transaction.deadline);
            unconfirmedTransaction.put("recipient", Convert.convert(transaction.recipient));
            unconfirmedTransaction.put("amount", transaction.amount);
            unconfirmedTransaction.put("fee", transaction.fee);
            unconfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));

            unconfirmedTransactions.add(unconfirmedTransaction);

        }

        for (Peer peer : Peer.allPeers) {

            String address = peer.peerAddress;

            if (peer.getBlacklistingTime() > 0) {

                JSONObject blacklistedPeer = new JSONObject();
                blacklistedPeer.put("index", peer.index);
                blacklistedPeer.put("announcedAddress", peer.getAnnouncedAddress().length() > 0
                        ? (peer.getAnnouncedAddress().length() > 30
                        ? (peer.getAnnouncedAddress().substring(0, 30) + "...")
                        : peer.getAnnouncedAddress())
                        : address);
                if (Nxt.wellKnownPeers.contains(peer.getAnnouncedAddress())) {
                    blacklistedPeer.put("wellKnown", true);
                }
                blacklistedPeers.add(blacklistedPeer);

            } else if (peer.getState() == Peer.State.NON_CONNECTED) {

                if (peer.getAnnouncedAddress().length() > 0) {

                    JSONObject knownPeer = new JSONObject();
                    knownPeer.put("index", peer.index);
                    knownPeer.put("announcedAddress", peer.getAnnouncedAddress().length() > 30 ? (peer.getAnnouncedAddress().substring(0, 30) + "...") : peer.getAnnouncedAddress());
                    if (Nxt.wellKnownPeers.contains(peer.getAnnouncedAddress())) {
                        knownPeer.put("wellKnown", true);
                    }

                    knownPeers.add(knownPeer);

                }

            } else {

                JSONObject activePeer = new JSONObject();
                activePeer.put("index", peer.index);
                if (peer.getState() == Peer.State.DISCONNECTED) {

                    activePeer.put("disconnected", true);

                }
                activePeer.put("address", address.length() > 30 ? (address.substring(0, 30) + "...") : address);
                activePeer.put("announcedAddress", peer.getAnnouncedAddress().length() > 30 ? (peer.getAnnouncedAddress().substring(0, 30) + "...") : peer.getAnnouncedAddress());
                activePeer.put("weight", peer.getWeight());
                activePeer.put("downloaded", peer.getDownloadedVolume());
                activePeer.put("uploaded", peer.getUploadedVolume());
                activePeer.put("software", peer.getSoftware());
                if (Nxt.wellKnownPeers.contains(peer.getAnnouncedAddress())) {
                    activePeer.put("wellKnown", true);
                }
                activePeers.add(activePeer);
            }
        }

        Long blockId = Blockchain.getLastBlock().getId();
        int numberOfBlocks = 0;
        while (numberOfBlocks < 60) {

            numberOfBlocks++;

            Block block = Blockchain.getBlock(blockId);
            JSONObject recentBlock = new JSONObject();
            recentBlock.put("index", block.getIndex());
            recentBlock.put("timestamp", block.timestamp);
            recentBlock.put("numberOfTransactions", block.transactions.length);
            recentBlock.put("totalAmount", block.totalAmount);
            recentBlock.put("totalFee", block.totalFee);
            recentBlock.put("payloadLength", block.payloadLength);
            recentBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
            recentBlock.put("height", block.getHeight());
            recentBlock.put("version", block.version);
            recentBlock.put("block", block.getStringId());
            recentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000))
                    .divide(BigInteger.valueOf(Nxt.initialBaseTarget)));

            recentBlocks.add(recentBlock);

            if (blockId.equals(Genesis.GENESIS_BLOCK_ID)) {
                break;
            }

            blockId = block.previousBlock;

        }

        JSONObject response = new JSONObject();
        response.put("response", "processInitialData");
        response.put("version", Nxt.VERSION);
        if (unconfirmedTransactions.size() > 0) {

            response.put("unconfirmedTransactions", unconfirmedTransactions);

        }
        if (activePeers.size() > 0) {

            response.put("activePeers", activePeers);

        }
        if (knownPeers.size() > 0) {

            response.put("knownPeers", knownPeers);

        }
        if (blacklistedPeers.size() > 0) {

            response.put("blacklistedPeers", blacklistedPeers);

        }
        if (recentBlocks.size() > 0) {

            response.put("recentBlocks", recentBlocks);

        }

        return response;
    }
}
