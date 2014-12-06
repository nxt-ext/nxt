/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {

    var _currentModal = null;

    NRS.setApproveAmountFormToAccounts = function() {
        var accountsBtn = _currentModal.find(".approve-amount-box button.approve-amount-accounts-btn");
        var nxtBtn     = _currentModal.find(".approve-amount-box button.approve-amount-nxt-btn");
        var inputField = _currentModal.find(".approve-amount-box input[name=pendingQuorum]");

        if (!accountsBtn.hasClass("active")) {
            accountsBtn.addClass("active");
        }
        nxtBtn.removeClass("active");

        inputField.attr("type", "number");
        inputField.attr("value", "1");
        inputField.attr("min", "1");

        _currentModal.find("input[name=pendingVotingModel]").attr("value", "1");
    }

    NRS.setApproveAmountFormToNXT = function() {
        var accountsBtn = _currentModal.find(".approve-amount-box button.approve-amount-accounts-btn");
        var nxtBtn     = _currentModal.find(".approve-amount-box button.approve-amount-nxt-btn");
        var inputField = _currentModal.find(".approve-amount-box input[name=pendingQuorum]");

        if (!nxtBtn.hasClass("active")) {
            nxtBtn.addClass("active");
        }
        accountsBtn.removeClass("active");

        inputField.attr("type", "text");
        inputField.attr("value", "0");
        inputField.attr("min", "0");

        _currentModal.find("input[name=pendingVotingModel]").attr("value", "0");
    }

    NRS.initApproveDialog = function(modal) {
        _currentModal = modal;
        _currentModal.find(".tx-modal-approve").append($('#tx-modal-approve-template').children().clone());

        _currentModal.find('.tx-modal-approve a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            $(e.target).closest('.tx-modal-approve').find('.tab-pane input').prop('disabled', true);
            $(e.target).closest('.tx-modal-approve').find('.tab-pane.active input').prop('disabled', false);

            var $inputFields = $(e.target).closest('.tx-modal-approve').find("input[name=pendingWhitelisted], input[name=pendingBlacklisted]").not("[type=hidden]");

            $.each($inputFields, function() {
                if ($(this).hasClass("noMask")) {
                    $(this).mask("NXT-****-****-****-*****", {
                        "noMask": true
                    }).removeClass("noMask");
                } else {
                    $(this).mask("NXT-****-****-****-*****");
                }
            });
        });

        $(function () { 
            $("[data-toggle='popover']").popover(); 
        });

        _currentModal.find("#approve-add-account").click(function(e) {
            var clone = _currentModal.find(".form-group-accounts").first().clone();
            clone.find("input").val("");
            clone.appendTo(".tx-modal-approve .approve-accounts-box");
        });

        _currentModal.find(".approve-accounts-box").on("click", "button.btn.remove_account", function(e) {
            e.preventDefault();
            if (_currentModal.find(".approve-accounts-box .form-group-accounts").length == 1) {
                return;
            }
            $(this).closest("div.form-group-accounts").remove();
        });

        _currentModal.find(".approve-amount-box  button.approve-amount-accounts-btn").click(function(e) {
            NRS.setApproveAmountFormToAccounts();
        });

        _currentModal.find(".approve-amount-box  button.approve-amount-nxt-btn").click(function(e) {
            NRS.setApproveAmountFormToNXT();
        });

        NRS.setApproveAmountFormToAccounts();
    }

    return NRS;
}(NRS || {}, jQuery));