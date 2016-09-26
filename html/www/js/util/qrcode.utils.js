var NRS = (function (NRS) {

    NRS.scanQRCode = function(readerId, callback) {
        NRS.logConsole("request camera permission");
        if (NRS.isMobileApp()) {
            cordova.plugins.permissions.hasPermission(cordova.plugins.permissions.CAMERA, function(status) {
                checkCameraPermission(status, callback)
            }, null);
        } else {
            // desktop scanning
        }
    };

    function checkCameraPermission(status, callback) {
        if(!status.hasPermission) {
            var errorCallback = function() {
                NRS.logConsole('Camera permission not granted');
            };

            NRS.logConsole('Request camera permission');
            cordova.plugins.permissions.requestPermission(cordova.plugins.permissions.CAMERA, function(status) {
                if(!status.hasPermission) {
                    NRS.logConsole('Camera status has no permission');
                    errorCallback();
                    return;
                }
                scanImpl(callback);
            }, errorCallback);
            return;
        }
        NRS.logConsole('Camera already has permission');
        scanImpl(callback);
    }

    function scanImpl(callback) {
        try {
            NRS.logConsole("before scan");
            cordova.plugins.barcodeScanner.scan(function(result) {
                scanQRDone(result, callback)
            }, function (error) {
                NRS.logConsole(error);
            });
        } catch (e) {
            NRS.logConsole(e.message);
        }
    }

    function scanQRDone(result, callback) {
        NRS.logConsole("Scan result format: " + result.format);
        if (!result.cancelled && result.format == "QR_CODE") {
            NRS.logConsole("Scan complete, send result to callback");
            callback(result.text);
        } else {
            NRS.logConsole("Scan cancelled");
        }
    }

    return NRS;
}(NRS || {}));
