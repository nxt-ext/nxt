/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
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

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {
    var SUBTYPE_SHUFFLING_CREATION = 0;
    var SUBTYPE_SHUFFLING_REGISTRATION = 1;
    var SUBTYPE_SHUFFLING_PROCESSING = 2;
    var SUBTYPE_SHUFFLING_RECIPIENTS = 3;
    var SUBTYPE_SHUFFLING_VERIFICATION = 4;
    var SUBTYPE_SHUFFLING_CANCELLATION = 5;

    function isErrorResponse(response) {
        return response.errorCode || response.errorDescription || response.errorMessage || response.error;
    }

    function getErrorMessage(response) {
        return response.errorDescription || response.errorMessage || response.error;
    } 

    NRS.jsondata = NRS.jsondata||{};

    NRS.jsondata.participant = function (response) {
        return $.extend(response, {});
    };

    NRS.jsondata.shuffler = function (response) {
        return $.extend(response, {});
    };

    /**
     *  {
     *      shuffling: String,
     *      issuerRS: String,
     *      holding: String,
     *      holdingType: Number,
     *      assigneeRS: String,
     *      amount: String,
     *      blocksRemaining:Number,
     *      participantCount: Number,
     *      stage: Number,
     *      shufflingStateHash: String,
     *      recipientPublicKeys: String
     *  }
     */
    NRS.jsondata.shuffling = function (response) {
        return $.extend(response, {
            amountLabel: NRS.formatAmount(response.amount),
            canRegister: response.stage == 0,
            stageLabel: (function () {
                switch (response.stage) {
                    case 0: return 'REGISTRATION';
                    case 1: return 'PROCESSING';
                    case 2: return 'VERIFICATION';
                    case 3: return 'BLAME';
                    case 4: return 'CANCELLED';
                    case 5: return 'DONE';
                }
            })(),
            holdingTypeLabel: (function () {
                switch (response.holdingType) {
                    case 0: return 'NXT';
                    case 1: return 'ASSET';
                    case 2: return 'CURRENCY';
                }
            })(),
            formattedAmount: (function () {
                switch (response.holdingType) {
                    case 0: return NRS.formatAmount(response.amount);
                    case 1: return NRS.formatQuantity(response.amount, 0);
                    case 2: return NRS.formatQuantity(response.amount, 0);
                }
            })(),
            holdingFormatted: (function () {
                switch (response.holdingType) {
                    case 0: return '';
                    case 1: return '<a href="#" data-goto-asset="'+response.holding.escapeHTML()+'">'+response.holdingInfo.name.escapeHTML()+'</a>';
                    case 2: return '<a href="#" data-goto-currency="'+response.holding.escapeHTML()+'">'+response.holdingInfo.name.escapeHTML()+'</a>';
                }
            })()
        });
    };

    NRS.pages.shuffling = function () {};

    NRS.setup.shuffling = function() {
        var sidebarId = 'sidebar_shuffling';
        NRS.addTreeviewSidebarMenuItem({
            "id": sidebarId,
            "titleHTML": '<i class="fa fa-random"></i> <span data-i18n="shuffling">Shuffling</span>',
            "page": 'shuffling',
            "desiredPosition": 80
        });
        NRS.appendMenuItemToTSMenuItem(sidebarId, {
            "titleHTML": '<span data-i18n="my_shufflings">My Shufflers</span>',
            "type": 'PAGE',
            "page": 'my_shufflers'
        });
        NRS.appendMenuItemToTSMenuItem(sidebarId, {
            "titleHTML": '<span data-i18n="all_shufflings">All Shufflings</span>',
            "type": 'PAGE',
            "page": 'all_shufflings'
        });
        NRS.appendMenuItemToTSMenuItem(sidebarId, {
            "titleHTML": '<span data-i18n="my_shufflings">My Shufflings</span>',
            "type": 'PAGE',
            "page": 'my_shufflings'
        });
        NRS.appendMenuItemToTSMenuItem(sidebarId, {
            "titleHTML": '<span data-i18n="create_shuffling">Create Shuffling</span>',
            "type": 'MODAL',
            "modalId": 'm_shuffling_create_modal'
        });

        $('#m_shuffling_create_holding_type').change();
    };

    /**
     * Create shuffling modal holding type onchange listener.
     * Hides holding field unless type is asset or currency.
     */
    $('#m_shuffling_create_holding_type').change(function () {
        var holdingType = $("#m_shuffling_create_holding_type");
        if(holdingType.val() == "0") {
            $("#shuffling_asset_id_group").css("display", "none");
            $("#shuffling_ms_currency_group").css("display", "none");
            $('#m_shuffling_create_unit').html($.t('nxt_capital_letters'));
            $('#m_shuffling_create_amount').attr('name', 'shufflingAmountNXT');
        } if(holdingType.val() == "1") {
			$("#shuffling_asset_id_group").css("display", "inline");
			$("#shuffling_ms_currency_group").css("display", "none");
            $('#m_shuffling_create_unit').html($.t('quantity'));
            $('#m_shuffling_create_amount').attr('name', 'amountQNTf');
		} else if(holdingType.val() == "2") {
			$("#shuffling_asset_id_group").css("display", "none");
			$("#shuffling_ms_currency_group").css("display", "inline");
            $('#m_shuffling_create_unit').html($.t('units'));
            $('#m_shuffling_create_amount').attr('name', 'amountQNTf');
		}
    });

    NRS.forms.shufflingCreate = function($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        switch (data.holdingType) {
            case '0':
                delete data.holding;
                break;
            case '1':
                break;
            case '2':
                break;
        }
        if (data.finishHeight) {
            data.registrationPeriod = parseInt(data.finishHeight) - NRS.lastBlockHeight;
            delete data.finishHeight;
        }
        return {
            "data": data
        }
    };

    NRS.pages.all_shufflings = function () {
        var view = NRS.simpleview.get('all_shufflings_page', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            shufflings: []
        });
        NRS.sendRequest("getAllShufflings", { includeHoldingInfo:'true' }, 
            function(response) {
                if (isErrorResponse(response)) {
                    view.render({
                        errorMessage: getErrorMessage(response),
                        isLoading: false,
                        isEmpty: false
                    });
                    return;
                }
                view.shufflings.length = 0;
                response.shufflings.forEach(
                    function (d) { 
                        view.shufflings.push( NRS.jsondata.shuffling(d))
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.shufflings.length == 0
                });
            }
        );
        NRS.incoming.all_shufflings = function (transactions) {
            setTimeout(NRS.pages.all_shufflings, 0);
            // transactions.forEach(function (transaction) {
            //     switch (transaction.type) {
            //         case SUBTYPE_SHUFFLING_CREATION:
            //         case SUBTYPE_SHUFFLING_REGISTRATION:
            //         case SUBTYPE_SHUFFLING_PROCESSING:
            //         case SUBTYPE_SHUFFLING_RECIPIENTS:
            //         case SUBTYPE_SHUFFLING_VERIFICATION:
            //         case SUBTYPE_SHUFFLING_CANCELLATION:
            //     }
            // });
        }
    };

    NRS.pages.my_shufflers = function () {
        var view = NRS.simpleview.get('my_shufflers_page', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            shufflers: [],
            start: function (shuffler) {
                alert(JSON.stringify(arguments))
            }
        });
        NRS.sendRequest("getShufflers", { "account": NRS.account }, 
            function(response) {
                if (isErrorResponse(response)) {
                    view.render({
                        errorMessage: getErrorMessage(response),
                        isLoading: false,
                        isEmpty: false
                    });
                    return;
                }
                view.shufflers.length = 0;
                response.shufflers.forEach(
                    function (d) { 
                        view.shufflers.push( NRS.jsondata.shuffler(d) ); 
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.shufflers.length == 0
                });
            }
        );
    };

    NRS.pages.my_shufflings = function () {
        var view = NRS.simpleview.get('my_shufflings_page', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            shufflings: []
        });
        NRS.sendRequest("getAccountShufflings", { "account": NRS.account }, 
            function(response) {
                if (isErrorResponse(response)) {
                    view.render({
                        errorMessage: getErrorMessage(response),
                        isLoading: false,
                        isEmpty: false
                    });
                    return;
                }
                view.shufflings.length = 0;
                response.shufflings.forEach(
                    function (d) { 
                        view.shufflings.push( NRS.jsondata.shuffling(d) ); 
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.shufflings.length == 0
                });
            }
        );
    };

    $("#m_shuffling_create_modal").on("show.bs.modal", function() {
   		context = {
   			labelText: "Currency",
   			labelI18n: "currency",
   			inputCodeName: "shuffling_ms_code",
   			inputIdName: "holding",
   			inputDecimalsName: "shuffling_ms_decimals",
   			helpI18n: "add_currency_modal_help"
   		};
   		NRS.initModalUIElement($(this), '.shuffling_holding_currency', 'add_currency_modal_ui_element', context);

   		context = {
   			labelText: "Asset",
   			labelI18n: "asset",
   			inputIdName: "holding",
   			inputDecimalsName: "shuffling_asset_decimals",
   			helpI18n: "add_asset_modal_help"
   		};
   		NRS.initModalUIElement($(this), '.shuffling_holding_asset', 'add_asset_modal_ui_element', context);

   		var context = {
   			labelText: "Finish Height",
   			labelI18n: "finish_height",
   			helpI18n: "shuffling_finish_height_help",
   			inputName: "finishHeight",
   			initBlockHeight: NRS.lastBlockHeight + 720,
   			changeHeightBlocks: 10
   		};
   		NRS.initModalUIElement($(this), '.shuffling_finish_height', 'block_height_modal_ui_element', context);
   	});

    $("#m_shuffling_register_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var shufflingId = $invoker.data("shuffling");
        $("#register_shuffling_id").html(shufflingId);
        var shufflingFullHash = $invoker.data("shufflingfullhash");
        $("#register_shuffling_full_hash").val(shufflingFullHash);
    });

    var shufflerStartModal = $("#m_shuffler_start_modal");
    shufflerStartModal.on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var shufflingId = $invoker.data("shuffling");
        $("#shuffler_start_shuffling_id").html(shufflingId);
        var shufflingFullHash = $invoker.data("shufflingfullhash");
        $("#shuffler_start_shuffling_full_hash").val(shufflingFullHash);
    });

    NRS.forms.startShuffler = function ($modal) {
        var data = NRS.getFormData($modal.find("form:first"));
        if (data.recipientSecretPhrase) {
            data.recipientPublicKey = NRS.getPublicKey(data.recipientSecretPhrase);
            delete data.recipientSecretPhrase;
        }
        return {
            "data": data
        };
    };

    return NRS;

}(NRS || {}, jQuery));