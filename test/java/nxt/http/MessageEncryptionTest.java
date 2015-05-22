package nxt.http;

import nxt.Account;
import nxt.BlockchainTest;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;
import org.junit.Assert;
import org.junit.Test;

public class MessageEncryptionTest extends BlockchainTest {

    @Test
    public void encryptBytes() {
        byte[] data = { (byte)0x01, (byte)0x02, (byte)0xF1, (byte)0xF2 };
        EncryptedData encryptedData = encrypt(data);
        Assert.assertArrayEquals(data, decrypt(encryptedData));
    }

    @Test
    public void encryptText() {
        JSONStreamAware json = JSONResponses.INCORRECT_ALIAS;
        EncryptedData encryptedData = encrypt(Convert.toBytes(json.toString()));
        Assert.assertEquals(json.toString(), Convert.toString(decrypt(encryptedData)));
    }

    @Test
    public void encryptEmpty() {
        EncryptedData encryptedData = encrypt(Convert.toBytes(""));
        Assert.assertEquals("", Convert.toString(decrypt(encryptedData)));
    }

    private EncryptedData encrypt(byte[] data) {
        Account recipient = Account.getAccount(BOB.getPublicKey());
        if (recipient == null) {
            throw new IllegalStateException();
        }
        return recipient.encryptTo(data, ALICE.getSecretPhrase(), false);
    }

    private byte[] decrypt(EncryptedData encryptedData) {
        Account sender = ALICE.getAccount();
        return sender.decryptFrom(encryptedData, BOB.getSecretPhrase(), false);
    }

}
