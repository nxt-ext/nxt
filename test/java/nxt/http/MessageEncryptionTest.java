package nxt.http;

import nxt.Account;
import nxt.BlockchainTest;
import nxt.crypto.Crypto;
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

    @Test
    public void encryptEncryptedData() {
        byte[] bytes = { (byte)0x01, (byte)0x02, (byte)0xF1, (byte)0xF2 };
        EncryptedData encryptedData = encrypt(bytes);
        byte[] encryptedBytes = EncryptedData.marshalData(encryptedData);
        EncryptedData encryptedData2 = encrypt(encryptedBytes);
        byte[] decryptedBytes2 = decrypt(encryptedData2);
        Assert.assertArrayEquals(encryptedBytes, decryptedBytes2);
        EncryptedData decryptedData2 = EncryptedData.unmarshalData(decryptedBytes2);
        byte[] decryptedBytes = decrypt(decryptedData2);
        Assert.assertArrayEquals(bytes, decryptedBytes);
    }


    private EncryptedData encrypt(byte[] data) {
        Account recipient = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        return recipient.encryptTo(data, secretPhrase1);
    }

    private byte[] decrypt(EncryptedData encryptedData) {
        Account sender = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        return sender.decryptFrom(encryptedData, secretPhrase2);
    }

}
