const url = "http://mclyaf03:6876";
const secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";

var bridge = require("./nrs.node.bridge").init({
    url: url,
    secretPhrase: secretPhrase,
    isTestNet: true
});

bridge.load(function(NRS) {
    var data = {
        recipient: NRS.accountRS,
        amountNQT: NRS.convertToNQT("1.234"),
        secretPhrase: secretPhrase
    };
    var messageToSelf = "hello world";
    data = Object.assign(data, bridge.getMandatoryParams(), bridge.encryptToSelf(NRS, messageToSelf, secretPhrase));
    NRS.sendRequest("sendMoney", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
