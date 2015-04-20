package nxt;

public interface Fee {

    long getFee(TransactionImpl transaction, Appendix appendage);

    Fee DEFAULT_FEE = new Fee.ConstantFee(Constants.ONE_NXT);

    Fee NONE = (transaction, appendage) -> 0;

    final class ConstantFee implements Fee {

        private final long fee;

        public ConstantFee(long fee) {
            this.fee = fee;
        }

        @Override
        public long getFee(TransactionImpl transaction, Appendix appendage) {
            return fee;
        }

    }

    abstract class SizeBasedFee implements Fee {

        private final long constantFee;
        private final long feePerKByte;

        public SizeBasedFee(long feePerKByte) {
            this(0, feePerKByte);
        }

        public SizeBasedFee(long constantFee, long feePerKByte) {
            this.constantFee = constantFee;
            this.feePerKByte = feePerKByte;
        }

        // the first 1024 bytes are free
        @Override
        public final long getFee(TransactionImpl transaction, Appendix appendage) {
            return Math.addExact(constantFee, Math.multiplyExact((long) (getSize(transaction, appendage) / 1024), feePerKByte));
        }

        public abstract int getSize(TransactionImpl transaction, Appendix appendage);

    }

}
