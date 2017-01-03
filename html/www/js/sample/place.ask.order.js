const url = "http://mclyaf03:6876";
const secretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";

try {
    var bridge = require("./../nrs.node.bridge.js"); // during development
} catch(e) {
    console.log("Release mode");
}

try {
    bridge = require("nxt-blockchain"); // when using the NPM module
} catch(e) {
    console.log("Development mode");
}

bridge.init({
    url: url,
    secretPhrase: secretPhrase,
    isTestNet: true
});

bridge.load(function(NRS) {
    const decimals = 2;
    var quantity = 2.5;
    var price = 1.3;
    var data = {
        asset: "6094526212840718212", // testnet Megasset
        quantityQNT: NRS.convertToQNT(quantity, decimals),
        priceNQT: NRS.calculatePricePerWholeQNT(NRS.convertToNQT(price), decimals),
        secretPhrase: secretPhrase
    };
    data = Object.assign(
        data,
        NRS.getMandatoryParams()
    );
    NRS.sendRequest("placeAskOrder", data, function (response) {
        NRS.logConsole(JSON.stringify(response));
    });
});
