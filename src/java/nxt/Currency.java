package nxt;

public interface Currency {

    long getBalance();

    long getUnconfirmedBalance();

    void addToBalance(long amount);

    void addToUnconfirmedBalance(long amount);

    void addToBalanceAndUnconfirmedBalance(long amount);

}
