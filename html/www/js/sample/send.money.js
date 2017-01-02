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
        recipient: NRS.getAccountIdFromPublicKey(recipientPublicKey), // public key to account id
        recipientPublicKey: recipientPublicKey, // Optional - public key announcement to init a new account
        amountNQT: NRS.convertToNQT("1.234"), // NXT to NQT conversion
        secretPhrase: secretPhrase,
        encryptedMessageIsPrunable: "true" // Optional - make the attached message prunable
    };
    // Compose the request data
    data = Object.assign(
        data,
        bridge.getMandatoryParams(),
        bridge.encryptMessage(NRS, "note to myself", secretPhrase, NRS.getPublicKey(converters.stringToHexString(secretPhrase)), true),
        bridge.encryptMessage(NRS, "message to recipient", secretPhrase, recipientPublicKey, false)
    );
    // Submit the request to the remote node using the standard client function which performs local signing for transactions
    // and validates the data returned from the server.
    // This method will only send the passphrase to the server in requests for which the passphrase is required like startForging
    // It will never submit the passphrase for transaction requests
    NRS.sendRequest("sendMoney", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
