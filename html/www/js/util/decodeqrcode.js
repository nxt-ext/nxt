/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

qrcode = {};
qrcode.callback = null;

qrcode.decode = function() {
    var canvasElem = $('#qr-canvas');
    var canvas = canvasElem[0];
    var dataurl = canvas.toDataURL('image/jpeg');
    var regex = /base64,(.*)/;
    var base64Array = regex.exec(dataurl);
    if(base64Array == null) {
        return;
    }
    var base64 = base64Array[1];
    NRS.sendRequest("decodeQRCode", { "qrCodeBase64": base64 },
        function(response) {
            if(qrcode.callback != null && 'qrCodeData' in response)
                if(response.qrCodeData == "") {
                    return;
                }
                qrcode.callback(response.qrCodeData);
        },
        false
    );
};
