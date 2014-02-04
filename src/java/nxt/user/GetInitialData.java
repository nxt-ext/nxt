package nxt.user;

import nxt.Block;
import nxt.Blockchain;
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
import java.util.List;

final class GetInitialData extends UserRequestHandler {

    static final GetInitialData instance = new GetInitialData();

    private GetInitialData() {}

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {

        JSONArray unconfirmedTransactions = new JSONArray();
        JSONArray activePeers = new JSONArray(), knownPeers = new JSONArray(), blacklistedPeers = new JSONArray();
        JSONArray recentBlocks = new JSONArray();

        for (Transaction transaction : Blockchain.getAllUnconfirmedTransactions()) {

            JSONObject unconfirmedTransaction = new JSONObject();
            unconfirmedTransaction.put("index", transaction.getIndex());
            unconfirmedTransaction.put("timestamp", transaction.getTimestamp());
            unconfirmedTransaction.put("deadline", transaction.getDeadline());
            unconfirmedTransaction.put("recipient", Convert.convert(transaction.getRecipientId()));
            unconfirmedTransaction.put("amount", transaction.getAmount());
            unconfirmedTransaction.put("fee", transaction.getFee());
            unconfirmedTransaction.put("sender", Convert.convert(transaction.getSenderAccountId()));

            unconfirmedTransactions.add(unconfirmedTransaction);

        }

        for (Peer peer : Peer.getAllPeers()) {

            String address = peer.getPeerAddress();

            if (peer.getBlacklistingTime() > 0) {

                JSONObject blacklistedPeer = new JSONObject();
                blacklistedPeer.put("index", peer.getIndex());
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
                    knownPeer.put("index", peer.getIndex());
                    knownPeer.put("announcedAddress", peer.getAnnouncedAddress().length() > 30 ? (peer.getAnnouncedAddress().substring(0, 30) + "...") : peer.getAnnouncedAddress());
                    if (Nxt.wellKnownPeers.contains(peer.getAnnouncedAddress())) {
                        knownPeer.put("wellKnown", true);
                    }

                    knownPeers.add(knownPeer);

                }

            } else {

                JSONObject activePeer = new JSONObject();
                activePeer.put("index", peer.getIndex());
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

        int height = Blockchain.getLastBlock().getHeight();
        List<Block> lastBlocks = Blockchain.getBlocksFromHeight(Math.max(0, height - 59));

        for (int i = lastBlocks.size() - 1; i >=0; i--) {
            Block block = lastBlocks.get(i);
            JSONObject recentBlock = new JSONObject();
            recentBlock.put("index", block.getIndex());
            recentBlock.put("timestamp", block.getTimestamp());
            recentBlock.put("numberOfTransactions", block.getTransactionIds().length);
            recentBlock.put("totalAmount", block.getTotalAmount());
            recentBlock.put("totalFee", block.getTotalFee());
            recentBlock.put("payloadLength", block.getPayloadLength());
            recentBlock.put("generator", Convert.convert(block.getGeneratorAccountId()));
            recentBlock.put("height", block.getHeight());
            recentBlock.put("version", block.getVersion());
            recentBlock.put("block", block.getStringId());
            recentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000))
                    .divide(BigInteger.valueOf(Nxt.initialBaseTarget)));

            recentBlocks.add(recentBlock);
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
