/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.showRawTransactionModal = function(transaction) {
		$("#raw_transaction_modal_unsigned_transaction_bytes").val(transaction.unsignedTransactionBytes);
		$("#raw_transaction_modal_transaction_bytes").val(transaction.transactionBytes);

		if (transaction.fullHash) {
			$("#raw_transaction_modal_full_hash").val(transaction.fullHash);
			$("#raw_transaction_modal_full_hash_container").show();
		} else {
			$("#raw_transaction_modal_full_hash_container").hide();
		}

		if (transaction.signatureHash) {
			$("#raw_transaction_modal_signature_hash").val(transaction.signatureHash);
			$("#raw_transaction_modal_signature_hash_container").show();
		} else {
			$("#raw_transaction_modal_signature_hash_container").hide();
		}

		$("#raw_transaction_modal").modal("show");
	}

	NRS.initAdvancedModalFormValues = function($modal) {
		$(".pending_number_accounts_group").find("input[name=phasingQuorum]").val(1);

		var context = {
			labelText: "Finish Height",
			labelI18n: "finish_height",
			helpI18n: "approve_transaction_finish_height_help",
			inputName: "phasingFinishHeight",
			initBlockHeight: NRS.lastBlockHeight + 7000,
			changeHeightBlocks: 500
		}
		var $elems = NRS.initModalUIElement($modal, '.pending_finish_height_group', 'block_height_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);

		var context = {
			labelText: "Amount NXT",
			labelI18n: "amount_nxt",
			helpI18n: "approve_transaction_amount_help",
			inputName: "phasingQuorumNXT",
			addonText: "NXT",
			addonI18n: "nxt_unit"
		}
		var $elems = NRS.initModalUIElement($modal, '.approve_transaction_amount_nxt', 'simple_input_with_addon_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);

		var context = {
			labelText: "Asset Quantity",
			labelI18n: "asset_quantity",
			helpI18n: "approve_transaction_amount_help",
			inputName: "phasingQuorumQNTf",
			addonText: "Quantity",
			addonI18n: "quantity"
		}
		var $elems = NRS.initModalUIElement($modal, '.approve_transaction_asset_quantity', 'simple_input_with_addon_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);

		var context = {
			labelText: "Currency Units",
			labelI18n: "currency_units",
			helpI18n: "approve_transaction_amount_help",
			inputName: "phasingQuorumQNTf",
			addonText: "Units",
			addonI18n: "units"
		}
		var $elems = NRS.initModalUIElement($modal, '.approve_transaction_currency_units', 'simple_input_with_addon_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);

		var context = {
			labelText: "Accounts (Whitelist)",
			labelI18n: "accounts_whitelist",
			helpI18n: "approve_transaction_accounts_requested_help",
			inputName: "phasingWhitelisted"
		}
		var $elems = NRS.initModalUIElement($modal, '.add_approval_whitelist_group', 'multi_accounts_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);

		var context = {
			labelText: "Min Balance Type",
			labelI18n: "min_balance_type",
			helpI18n: "approve_transaction_min_balance_type_help",
			selectName: "phasingMinBalanceModel"
		}
		var $elems = NRS.initModalUIElement($modal, '.approve_min_balance_model_group', 'min_balance_model_modal_ui_element', context);
		$elems.find('select').prop("disabled", true);

		$elems.each(function(e) {
			var $mbGroup = $(this).closest('div.approve_min_balance_model_group');
			if ($mbGroup.hasClass("approve_mb_balance")) {
				$mbGroup.find('option[value="1"], option[value="2"], option[value="3"]').remove();
			}
			if ($mbGroup.hasClass("approve_mb_asset")) {
				$mbGroup.find('option[value="1"], option[value="3"]').remove();
			}
			if ($mbGroup.hasClass("approve_mb_currency")) {
				$mbGroup.find('option[value="1"], option[value="2"]').remove();
			}
		});

		var context = {
			labelText: "Min Balance",
			labelI18n: "min_balance",
			helpI18n: "approve_transaction_min_balance_help",
			inputName: "",
			addonText: "",
			addonI18n: ""
		}
		context['inputName'] = 'phasingMinBalanceNXT';
		context['addonText'] = 'NXT';
		context['addonI18n'] = 'nxt_unit';
		var $elems = NRS.initModalUIElement($modal, '.approve_min_balance_nxt', 'simple_input_with_addon_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems.hide();

		context['inputName'] = 'phasingMinBalanceQNTf';
		context['addonText'] = 'Quantity';
		context['addonI18n'] = 'quantity';
		var $elems = NRS.initModalUIElement($modal, '.approve_min_balance_asset_quantity', 'simple_input_with_addon_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems.hide();

		context['inputName'] = 'phasingMinBalanceQNTf';
		context['addonText'] = 'Units';
		context['addonI18n'] = 'units';
		var $elems = NRS.initModalUIElement($modal, '.approve_min_balance_currency_units', 'simple_input_with_addon_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems.hide();

		context = {
			labelText: "Asset",
			labelI18n: "asset",
			inputIdName: "phasingHolding",
			inputDecimalsName: "phasingHoldingDecimals",
			helpI18n: "add_asset_modal_help"
		}
		$elems = NRS.initModalUIElement($modal, '.approve_holding_asset', 'add_asset_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems = NRS.initModalUIElement($modal, '.approve_holding_asset_optional', 'add_asset_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems.hide();

		context = {
			labelText: "Currency",
			labelI18n: "currency",
			inputCodeName: "phasingHoldingCurrencyCode",
			inputIdName: "phasingHolding",
			inputDecimalsName: "phasingHoldingDecimals",
			helpI18n: "add_currency_modal_help"
		}
		$elems = NRS.initModalUIElement($modal, '.approve_holding_currency', 'add_currency_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems = NRS.initModalUIElement($modal, '.approve_holding_currency_optional', 'add_currency_modal_ui_element', context);
		$elems.find('input').prop("disabled", true);
		$elems.hide();
	}

	function _setApprovalFeeAddition() {
		var feeAddition = $('.modal:visible .approve_tab_list li.active a').data("feeNxtApprovalAddition");
		var $mbSelect = $('.modal:visible .tab_pane_approve.active .approve_min_balance_model_group select');
		if($mbSelect.length > 0 && $mbSelect.val() != "0") {
			feeAddition = String(20);
		}

        $('.modal:visible').find("input[name='feeNXT_approval_addition']").val(feeAddition);
        $('.modal:visible').find("span.feeNXT_approval_addition_info").html("+" + feeAddition);
	}

	$('.approve_tab_list a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		_setApprovalFeeAddition();
        $am = $(this).closest('.approve_modal');
        $am.find('.tab-pane input, .tab-pane select').prop('disabled', true);
        $am.find('.tab-pane.active input, .tab-pane.active select').prop('disabled', false);
        if ($(this).hasClass("at_no_approval")) {
			$am.find('.approve_whitelist_accounts').hide();
        	$am.find('.approve_whitelist_accounts input').prop('disabled', true);
        } else {
        	$am.find('.approve_whitelist_accounts input').prop('disabled', false);
        	$am.find('.approve_whitelist_accounts').show();
        }
        $('.modal .approve_modal .approve_min_balance_model_group:visible select').trigger('change');
    });

	$('body').on('change', '.modal .approve_modal .approve_min_balance_model_group select', function(e) {
		_setApprovalFeeAddition();
		var $tabPane = $(this).closest('div.tab_pane_approve');
		var mbModelId = $(this).val();
		for(var id=0; id<=3; id++) {
			$tabPane.find('.approve_mb_model_' + String(id) + ' input').attr('disabled', true);
			$tabPane.find('.approve_mb_model_' + String(id)).hide();
		}
		$tabPane.find('.approve_mb_model_' + String(mbModelId) + ' input').attr('disabled', false);
		$tabPane.find('.approve_mb_model_' + String(mbModelId)).show();
	});

	$("#transaction_operations_modal").on("show.bs.modal", function(e) {
		$(this).find(".output_table tbody").empty();
		$(this).find(".output").hide();

		$(this).find(".tab_content:first").show();
		$("#transaction_operations_modal_button").text($.t("broadcast")).data("resetText", $.t("broadcast")).data("form", "broadcast_transaction_form");
	});

	$("#transaction_operations_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".tab_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$(this).find("ul.nav li:first").addClass("active");

		$(this).find(".output_table tbody").empty();
		$(this).find(".output").hide();
	});

	$("#transaction_operations_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(this).closest(".modal").find(".tab_content").hide();

		if (tab == "broadcast_transaction") {
			$("#transaction_operations_modal_button").text($.t("broadcast")).data("resetText", $.t("broadcast")).data("form", "broadcast_transaction_form");
		} else if (tab == "parse_transaction") {
			$("#transaction_operations_modal_button").text($.t("parse_transaction_bytes")).data("resetText", $.t("parse_transaction_bytes")).data("form", "parse_transaction_form");
		} else {
			$("#transaction_operations_modal_button").text($.t("calculate_full_hash")).data("resetText", $.t("calculate_full_hash")).data("form", "calculate_full_hash_form");
		}

		$("#transaction_operations_modal_" + tab).show();
	});

	NRS.forms.broadcastTransactionComplete = function(response, data) {
		$("#parse_transaction_form").find(".error_message").hide();
		$("#transaction_operations_modal").modal("hide");
	}

	NRS.forms.parseTransactionComplete = function(response, data) {
		$("#parse_transaction_form").find(".error_message").hide();
		$("#parse_transaction_output_table tbody").empty().append(NRS.createInfoTable(response, true));
		$("#parse_transaction_output").show();
	}

	NRS.forms.parseTransactionError = function() {
		$("#parse_transaction_output_table tbody").empty();
		$("#parse_transaction_output").hide();
	}

	NRS.forms.calculateFullHashComplete = function(response, data) {
		$("#calculate_full_hash_form").find(".error_message").hide();
		$("#calculate_full_hash_output_table tbody").empty().append(NRS.createInfoTable(response, true));
		$("#calculate_full_hash_output").show();
	}

	NRS.forms.calculateFullHashError = function() {
		$("#calculate_full_hash_output_table tbody").empty();
		$("#calculate_full_hash_output").hide();
	}

	return NRS;
}(NRS || {}, jQuery));