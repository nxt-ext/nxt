import nxt.util.Convert;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigInteger;

// used only to convert old serialized files, to be deleted after 0.6.0
final class Nxt {

    static class Transaction implements Serializable {

        static final long serialVersionUID = 0;

        byte type, subtype;
        int timestamp;
        short deadline;
        byte[] senderPublicKey;
        long recipient;
        int amount, fee;
        long referencedTransaction;
        byte[] signature;
        nxt.Attachment attachment;

        int index;
        long block;
        int height;

        public Object readResolve() throws ObjectStreamException {

            nxt.Transaction transaction = attachment != null
                            ? nxt.Transaction.newTransaction(timestamp, deadline, senderPublicKey, recipient,
                            amount, fee, Convert.zeroToNull(referencedTransaction), attachment)
                            : nxt.Transaction.newTransaction(timestamp, deadline, senderPublicKey, recipient,
                            amount, fee, Convert.zeroToNull(referencedTransaction));
            transaction.signature = signature;
            transaction.index = index;
            transaction.block = Convert.zeroToNull(block);
            transaction.height = height;
            return transaction;
        }


        public static interface Attachment {

        }

        static class MessagingArbitraryMessageAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            byte[] message;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.MessagingArbitraryMessage(message);
            }

        }

        static class MessagingAliasAssignmentAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            String alias;
            String uri;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.MessagingAliasAssignment(alias, uri);
            }
        }

        static class ColoredCoinsAssetIssuanceAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            String name;
            String description;
            int quantity;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsAssetIssuance(name, description, quantity);
            }


        }

        static class ColoredCoinsAssetTransferAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long asset;
            int quantity;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsAssetTransfer(Convert.zeroToNull(asset), quantity);
            }


        }

        static class ColoredCoinsAskOrderPlacementAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long asset;
            int quantity;
            long price;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsAskOrderPlacement(Convert.zeroToNull(asset), quantity, price);
            }


        }

        static class ColoredCoinsBidOrderPlacementAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long asset;
            int quantity;
            long price;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsBidOrderPlacement(Convert.zeroToNull(asset), quantity, price);
            }


        }

        static class ColoredCoinsAskOrderCancellationAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long order;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(order));
            }


        }

        static class ColoredCoinsBidOrderCancellationAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long order;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(order));
            }


        }
    }

    static class Block implements Serializable {

        static final long serialVersionUID = 0;
        static final long[] emptyLong = new long[0];

        int version;
        int timestamp;
        long previousBlock;
        int totalAmount, totalFee;
        int payloadLength;
        byte[] payloadHash;
        byte[] generatorPublicKey;
        byte[] generationSignature;
        byte[] blockSignature;

        byte[] previousBlockHash;

        int index;
        long[] transactions;
        long baseTarget;
        int height;
        long nextBlock;
        BigInteger cumulativeDifficulty;

        public Object readResolve() throws ObjectStreamException {

            nxt.Block block = new nxt.Block(version, timestamp, Convert.zeroToNull(previousBlock), transactions.length,
                    totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
            block.index = index;
            for (int i = 0 ; i < transactions.length; i++) {
                block.transactions[i] = transactions[i];
            }
            block.baseTarget = baseTarget;
            block.height = height;
            block.nextBlock = Convert.zeroToNull(nextBlock);
            block.cumulativeDifficulty = cumulativeDifficulty;
            return block;
        }
    }
}