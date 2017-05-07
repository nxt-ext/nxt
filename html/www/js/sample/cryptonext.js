var loader = require("./loader");
var config = loader.config;

loader.load(function(NRS) {
    NRS = Object.assign(NRS, require('../nrs.cryptonext'));
    NRS.getInfo();
});