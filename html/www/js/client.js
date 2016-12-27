const url = "http://mclyaf03:6876";
const secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
const deadline = "1440"; // must be numeric value of type string
require("./nrs.node.bridge").init({
    url: url,
    secretPhrase: secretPhrase,
    isTestNet: true
}).get(function(NRS) {
    var encrypted = NRS.encryptNote("hello world", {
        "publicKey": converters.hexStringToByteArray(NRS.generatePublicKey(secretPhrase))
    }, secretPhrase);

    var encryptToSelfMessageData = encrypted.message;
    var encryptToSelfMessageNonce = encrypted.nonce;
    NRS.sendRequest("sendMoney", {
        recipient: NRS.accountRS,
        amountNQT: "123456789",
        secretPhrase: secretPhrase,
        deadline: deadline,
        feeNQT: 0,
        encryptToSelfMessageData: encryptToSelfMessageData,
        encryptToSelfMessageNonce: encryptToSelfMessageNonce,
        messageToEncryptToSelfIsText: "true"
    }, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
