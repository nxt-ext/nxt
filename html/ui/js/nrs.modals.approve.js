/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {

    var _currentModal = null;


    NRS.initApproveDialog = function(modal) {
        _currentModal = modal;
    }

    return NRS;
}(NRS || {}, jQuery));