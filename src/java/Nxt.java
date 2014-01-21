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

            nxt.Transaction transaction = new nxt.Transaction(type, subtype, timestamp, deadline, senderPublicKey, recipient, amount, fee, referencedTransaction, signature);
            transaction.attachment = attachment;
            transaction.index = index;
            transaction.block = block;
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
                return new nxt.Attachment.ColoredCoinsAssetTransfer(asset, quantity);
            }


        }

        static class ColoredCoinsAskOrderPlacementAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long asset;
            int quantity;
            long price;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, price);
            }


        }

        static class ColoredCoinsBidOrderPlacementAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long asset;
            int quantity;
            long price;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
            }


        }

        static class ColoredCoinsAskOrderCancellationAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long order;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsAskOrderCancellation(order);
            }


        }

        static class ColoredCoinsBidOrderCancellationAttachment implements Attachment, Serializable {

            static final long serialVersionUID = 0;

            long order;

            public Object readResolve() throws ObjectStreamException {
                return new nxt.Attachment.ColoredCoinsBidOrderCancellation(order);
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

            nxt.Block block = new nxt.Block(version, timestamp, previousBlock, transactions.length, totalAmount, totalFee, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash);
            block.index = index;
            System.arraycopy(transactions, 0, block.transactions, 0, transactions.length);
            block.baseTarget = baseTarget;
            block.height = height;
            block.nextBlock = nextBlock;
            block.cumulativeDifficulty = cumulativeDifficulty;
            return block;
        }
    }
}