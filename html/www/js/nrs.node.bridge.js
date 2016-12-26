var isDone = false;
require("jsdom").env("", function(err, window) {
    try {
        if (err) {
            console.error(err);
            return;
        }
        console.log("Started");
        global.jQuery = require("jquery")(window);
        global.$ = global.jQuery;
        global.window = window;
        window.console = console;
        global.document = {};
        require('./util/extensions');
        require('./util/converters');
        global.isNode = true;
        global.server = {};
        global.server = Object.assign(server, require('./nrs.constants'));
        global.server = Object.assign(server, require('./nrs.feature.detection'));
        global.server = Object.assign(server, require('./nrs.console'));
        global.server = Object.assign(server, require('./nrs.util'));
        server.getRemoteNodeUrl = function () {
            return "http://localhost:6876";
        };
        global.server = Object.assign(server, require('./nrs'));
        server.accountInfo = {};
        global.server = Object.assign(server, require('./nrs.server'));
        server.sendRequest("getBlockchainStatus", {}, function (response) {
            server.logConsole(JSON.stringify(response));
            isDone = true;
        });
    } catch (e) {
        console.log(e.message);
        console.log(e.stack);
    } finally {
        isDone = true;
        console.log("Almost done");
    }
    (function wait() {
        if (!isDone) {
            setTimeout(wait, 1000);
        }
        console.log("Done");
    })();
});