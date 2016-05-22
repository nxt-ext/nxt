QUnit.module("nrs.encryption");

QUnit.test("generatePublicKey", function (assert) {
    assert.throws(function() { NRS.generatePublicKey("") }, "Can't generate public key without the user's password.", "empty.public.key");
    assert.equal(NRS.generatePublicKey("12345678"), "a65ae5bc3cdaa9a0dd66f2a87459bbf663140060e99ae5d4dfe4dbef561fdd37", "public.key");
    assert.equal(NRS.generatePublicKey("hope peace happen touch easy pretend worthless talk them indeed wheel state"), "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b", "public.key");
});

QUnit.test("getPublicKey", function (assert) {
    var publicKey1 = NRS.getPublicKey(converters.stringToHexString("12345678"));
    assert.equal(publicKey1, "a65ae5bc3cdaa9a0dd66f2a87459bbf663140060e99ae5d4dfe4dbef561fdd37", "public.key");
});

QUnit.test("getAccountIdFromPublicKey", function (assert) {
    assert.equal(NRS.getAccountIdFromPublicKey("112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b", true), "NXT-XK4R-7VJU-6EQG-7R335", "account.rs");
    assert.equal(NRS.getAccountIdFromPublicKey("112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b", false), "5873880488492319831", "account.rs");
});

QUnit.test("getPrivateKey", function (assert) {
    assert.equal(NRS.getPrivateKey("12345678"), "e8797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f", "private.key");
});

QUnit.test("encryptDecryptNote", function (assert) {
    var senderPrivateKey = "rshw9abtpsa2";
    var senderPublicKeyHex = NRS.getPublicKey(converters.stringToHexString(senderPrivateKey));
    var receiverPrivateKey = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    var receiverPublicKeyHex = NRS.getPublicKey(converters.stringToHexString(receiverPrivateKey));
    var encryptedNote = NRS.encryptNote("MyMessage", { publicKey: receiverPublicKeyHex }, senderPrivateKey);
    assert.equal(encryptedNote.message.length, 96, "message.length");
    assert.equal(encryptedNote.nonce.length, 64, "nonce.length");
    var decryptedNote = NRS.decryptNote(encryptedNote.message, {
        nonce: encryptedNote.nonce,
        publicKey: converters.hexStringToByteArray(senderPublicKeyHex)
    }, receiverPrivateKey);
    assert.equal(decryptedNote, "MyMessage", "decrypted");
});

QUnit.test("encryptDecryptNote", function (assert) {
    var senderPassphrase = "rshw9abtpsa2";
    var senderPublicKeyHex = NRS.getPublicKey(converters.stringToHexString(senderPassphrase));
    var senderPrivateKeyHex = NRS.getPrivateKey(senderPassphrase);
    var receiverPassphrase = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    var receiverPublicKeyHex = NRS.getPublicKey(converters.stringToHexString(receiverPassphrase));
    var receiverPrivateKeyHex = NRS.getPrivateKey(receiverPassphrase);
    var encryptedNote = NRS.encryptDataRoof(converters.stringToByteArray("MyMessage"), {
        privateKey: converters.hexStringToByteArray(senderPrivateKeyHex),
        publicKey: converters.hexStringToByteArray(receiverPublicKeyHex)
    });
    assert.equal(encryptedNote.data.length, 48, "message.length");
    assert.equal(encryptedNote.nonce.length, 32, "nonce.length");
    var decryptedNote = NRS.decryptDataRoof(encryptedNote.data, {
        nonce: encryptedNote.nonce,
        privateKey: converters.hexStringToByteArray(receiverPrivateKeyHex),
        publicKey: converters.hexStringToByteArray(senderPublicKeyHex)
    });
    assert.equal(decryptedNote, "MyMessage", "decrypted");
});

// Based on testnet transaction 17867212180997536482
QUnit.test("getSharedKey", function (assert) {
    var privateKey = NRS.getPrivateKey("rshw9abtpsa2");
    var publicKey = "112e0c5748b5ea610a44a09b1ad0d2bddc945a6ef5edc7551b80576249ba585b";
    var nonce = "67c2be503505d8e6498cd108a5f37c624899dcdae025276d720f608e54cf3177";
    var nonceBytes = converters.hexStringToByteArray(nonce);
    var sharedKeyBytes = NRS.getSharedKey(converters.hexStringToByteArray(privateKey), converters.hexStringToByteArray(publicKey), nonceBytes);
    // Make sure it's the same key produced by the server getSharedKey API
    assert.equal(converters.byteArrayToHexString(sharedKeyBytes), "68dd970a1144cc7595c745541b0318b08aa6ccd8121e061b378fc27ffc5e1cd1");
    var options = {};
    options.sharedKey = sharedKeyBytes;
    var encryptedMessage = "8adee4dee3e3311a631a29553140d177932cf0743c05846d897b24545d6839cbf368fc0b0eec628bfd69e95d006e3eb8";
    var decryptedMessage = NRS.decryptDataRoof(converters.hexStringToByteArray(encryptedMessage), options);
    assert.equal(decryptedMessage, "hello world");
});

