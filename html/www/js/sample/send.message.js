const url = "http://mclyaf03:6876";
const secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
const recipientPublicKey = "0b4e505972149e7ceb51309edc76729795cabe1f2cc42d87688138d0966db436";

var bridge = require("./../nrs.node.bridge.js"); // during development
try {
    bridge = require("nxt-blockchain"); // when using the NPM module
} catch(e) {
    console.log(e.message);
}

bridge.init({
    url: url,
    secretPhrase: secretPhrase,
    isTestNet: true
});

bridge.load(function(NRS) {
    var data = {
        recipient: NRS.getAccountIdFromPublicKey(recipientPublicKey),
        secretPhrase: secretPhrase,
        encryptedMessageIsPrunable: "true"
    };
    data = Object.assign(
        data,
        bridge.getMandatoryParams(),
        bridge.encryptMessage(NRS, "hello world", secretPhrase, recipientPublicKey, false)
    );
    NRS.sendRequest("sendMessage", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
