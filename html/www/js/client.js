const url = "http://mclyaf03:6876";
const secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
const deadline = "1440"; // must be numeric value of type string
require("./nrs.node.bridge").init({
    url: url,
    secretPhrase: secretPhrase,
    isTestNet: true
}).get(function(NRS) {
    NRS.sendRequest("sendMoney", {
        recipient: NRS.accountRS,
        amountNQT: "123456789",
        secretPhrase: secretPhrase,
        deadline: deadline,
        feeNQT: 0
    }, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
