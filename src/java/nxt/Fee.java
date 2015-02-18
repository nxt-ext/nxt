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

    public static final class SizeBasedFee implements Fee {

        private final long constantFee;
        private final long sizeBasedFee;

        public SizeBasedFee(long constantFee, long sizeBasedFee) {
            this.constantFee = constantFee;
            this.sizeBasedFee = sizeBasedFee;
        }

        @Override
        public long getFee(TransactionImpl transaction, Appendix appendage) {
            return Convert.safeAdd(constantFee, Convert.safeMultiply(appendage.getSize(), sizeBasedFee));
        }

    }

}
