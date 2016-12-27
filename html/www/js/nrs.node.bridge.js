const options = {
    url: "http://localhost:6876",
    secretPhrase: "",
    isTestNet: false
};

exports.init = function(params) {
    if (!params) {
        return;
    }
    options.url = params.url;
    options.secretPhrase = params.secretPhrase;
    options.isTestNet = params.isTestNet;
    return this;
};

exports.get = function(callback) {
    require("jsdom").env("", function(err, window) {
        try {
            if (err) {
                console.error(err);
                return;
            }
            console.log("Started");
            global.jQuery = require("jquery")(window);
            global.$ = global.jQuery;
            $.t = function(text) { return text; };
            global.crypto = require("crypto");
            global.CryptoJS = require("crypto-js");
            global.async = require("async");
            global.pako = require("pako");
            global.window = window;
            window.console = console;
            global.document = {};
            global.isNode = true;
            global.navigator = {};
            navigator.userAgent = "";
            global.NxtAddress = require('./util/nxtaddress');
            var jsbn = require('jsbn');
            global.BigInteger = jsbn.BigInteger;
            // require('./3rdparty/jsbn2');
            global.curve25519 = require('./crypto/curve25519');
            global.curve25519_ = require('./crypto/curve25519_');
            require('./util/extensions');
            global.converters = require('./util/converters');
            global.server = {};
            global.server.isTestNet = options.isTestNet;
            global.server = Object.assign(server, require('./nrs.encryption'));
            global.server = Object.assign(server, require('./nrs.feature.detection'));
            global.server = Object.assign(server, require('./nrs.transactions.types'));
            global.server = Object.assign(server, require('./nrs.constants'));
            global.server = Object.assign(server, require('./nrs.console'));
            global.server = Object.assign(server, require('./nrs.util'));
            server.getRemoteNodeUrl = function () {
                return options.url;
            };
            server.account = server.getAccountId(options.secretPhrase);
            server.accountRS = converters.convertNumericToRSAccountFormat(server.account);
            global.server = Object.assign(server, require('./nrs'));
            server.accountInfo = {};
            global.server = Object.assign(server, require('./nrs.server'));
            global.server = Object.assign(server, require('./nrs.constants'));
            var loadConstants = new Promise(function(resolve) {
                server.loadServerConstants(resolve);
            });
            loadConstants.then(function() {
                callback(global.server);
            }).catch(function(e) {
                console.log("loadConstants failed");
                console.log(e.message);
                console.log(e.stack);
            });
        } catch (e) {
            console.log(e.message);
            console.log(e.stack);
        }
    });
};