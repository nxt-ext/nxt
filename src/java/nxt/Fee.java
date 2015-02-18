package nxt;

import nxt.util.Convert;

public interface Fee {

    long getFee(TransactionImpl transaction, Appendix appendage);

    public static final Fee DEFAULT_FEE = new Fee.ConstantFee(Constants.ONE_NXT);

    public static final Fee NONE = new Fee() {
        @Override
        public long getFee(TransactionImpl transaction, Appendix appendage) {
            return 0;
        }
    };

    public static final class ConstantFee implements Fee {

        private final long fee;

        public ConstantFee(long fee) {
            this.fee = fee;
        }

        @Override
        public long getFee(TransactionImpl transaction, Appendix appendage) {
            return fee;
        }

    }

    public static abstract class SizeBasedFee implements Fee {

        private final long feePerKByte;

        public SizeBasedFee(long feePerKByte) {
            this.feePerKByte = feePerKByte;
        }

        // the first 1024 bytes are free
        @Override
        public final long getFee(TransactionImpl transaction, Appendix appendage) {
            return Convert.safeMultiply(getSize(transaction, appendage) / 1024, feePerKByte);
        }

        public abstract int getSize(TransactionImpl transaction, Appendix appendage);

    }

}
