package nxt;

import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.http.JSONResponses;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;
import org.junit.Assert;
import org.junit.Test;

public class MessageEncryptionTest extends BlockchainTest {

    @Test
    public void encryptBytes() {
        byte[] data = { (byte)0x01, (byte)0x02, (byte)0xF1, (byte)0xF2 };
        byte[] decryptedData = encryptDecrypt(data);
        Assert.assertArrayEquals(data, decryptedData);
    }

    @Test
    public void encryptText() {
        JSONStreamAware json = JSONResponses.INCORRECT_ALIAS;
        String decryptedText = Convert.toString(encryptDecrypt(Convert.toBytes(json.toString())));
        Assert.assertEquals(json.toString(), decryptedText);
    }

    @Test
    public void encryptEmpty() {
        String decryptedText = Convert.toString(encryptDecrypt(Convert.toBytes("")));
        Assert.assertEquals("", decryptedText);
    }

    private byte[] encryptDecrypt(byte[] data) {
        Account sender = Account.getAccount(Crypto.getPublicKey(secretPhrase1));
        Account recipient = Account.getAccount(Crypto.getPublicKey(secretPhrase2));
        EncryptedData encryptedData = recipient.encryptTo(data, secretPhrase1);
        return sender.decryptFrom(encryptedData, secretPhrase2);
    }

}
