/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {

    var _currentModal = null;

    NRS.initApproveDialog = function(modal) {
        _currentModal = modal;
        _currentModal.find(".tx-modal-approve").append($('#tx-modal-approve-template').children().clone());

        _currentModal.find('.tx-modal-approve a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            $(e.target).closest('.tx-modal-approve').find('.tab-pane input').prop('disabled', true);
            $(e.target).closest('.tx-modal-approve').find('.tab-pane.active input').prop('disabled', false);
        });
    }

    return NRS;
}(NRS || {}, jQuery));