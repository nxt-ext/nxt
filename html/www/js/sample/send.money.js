const url = "http://mclyaf03:6876";
const secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
const recipientPublicKey = "0b4e505972149e7ceb51309edc76729795cabe1f2cc42d87688138d0966db436";

var bridge = require("./../nrs.node.bridge.js").init({
    url: url,
    secretPhrase: secretPhrase,
    isTestNet: true
});

bridge.load(function(NRS) {
    var data = {
        recipient: NRS.getAccountIdFromPublicKey(recipientPublicKey),
        recipientPublicKey: recipientPublicKey, // Optional public key announcement to init a new account
        amountNQT: NRS.convertToNQT("1.234"),
        secretPhrase: secretPhrase,
        encryptedMessageIsPrunable: "true"
    };
    data = Object.assign(
        data,
        bridge.getMandatoryParams(),
        bridge.encryptMessage(NRS, "note to myself", secretPhrase, NRS.getPublicKey(converters.stringToHexString(secretPhrase)), true),
        bridge.encryptMessage(NRS, "message to recipient", secretPhrase, recipientPublicKey, false)
    );
    NRS.sendRequest("sendMoney", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
