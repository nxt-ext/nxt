var NRS = (function (NRS) {

    NRS.scanQRCode = function(readerId, callback) {
        NRS.logConsole("request camera permission");
        if (NRS.isCordovaScanningEnabled()) {
            cordova.plugins.permissions.hasPermission(cordova.plugins.permissions.CAMERA, function(status) {
                cordovaCheckCameraPermission(status, callback)
            }, null);
        } else {
            html5Scan(readerId, callback);
        }
    };

    function cordovaCheckCameraPermission(status, callback) {
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
                cordovaScan(callback);
            }, errorCallback);
            return;
        }
        NRS.logConsole('Camera already has permission');
        cordovaScan(callback);
    }

    function cordovaScan(callback) {
        try {
            NRS.logConsole("before scan");
            cordova.plugins.barcodeScanner.scan(function(result) {
                cordovaScanQRDone(result, callback)
            }, function (error) {
                NRS.logConsole(error);
            });
        } catch (e) {
            NRS.logConsole(e.message);
        }
    }

    function cordovaScanQRDone(result, callback) {
        NRS.logConsole("Scan result format: " + result.format);
        if (!result.cancelled && result.format == "QR_CODE") {
            NRS.logConsole("Scan complete, send result to callback");
            callback(result.text);
        } else {
            NRS.logConsole("Scan cancelled");
        }
    }

    function html5Scan(readerId, callback) {
        var reader = $("#" + readerId);
        if (reader.is(':visible')) {
            reader.hide();
            if (reader.data('stream')) {
                reader.html5_qrcode_stop();
            }
            return;
        }
        reader.empty();
        reader.show();
        reader.html5_qrcode(
            function (data) {
                NRS.logConsole(data);
                callback(data);
                reader.hide();
                reader.html5_qrcode_stop();
            },
            function (error) {},
            function (videoError, localMediaStream) {
                NRS.logConsole(videoError);
                reader.hide();
                if (!localMediaStream) {
                    $.growl("video_not_supported");
                }
                if (reader.data('stream')) {
                    reader.html5_qrcode_stop();
                }
            }
        );
    }

    return NRS;
}(NRS || {}));
