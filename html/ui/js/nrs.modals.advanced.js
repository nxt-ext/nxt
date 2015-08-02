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
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.showRawTransactionModal = function(transaction) {
		$("#raw_transaction_modal_unsigned_transaction_bytes").val(transaction.unsignedTransactionBytes);

        if (transaction.unsignedTransactionBytes && !transaction.transactionBytes) {
            $("#raw_transaction_modal_unsigned_bytes_qr_code").empty().qrcode({
                "text": transaction.unsignedTransactionBytes,
                "width": 384,
                "height": 384
            });
            $("#raw_transaction_modal_unsigned_bytes_qr_code_container").show();
        } else {
            $("#raw_transaction_modal_unsigned_bytes_qr_code_container").hide();
        }

        $("#raw_transaction_modal_unsigned_transaction_json").val(JSON.stringify(transaction.transactionJSON));

		if (transaction.transactionBytes) {
            $("#raw_transaction_modal_transaction_bytes").val(transaction.transactionBytes);
            $("#raw_transaction_modal_transaction_bytes_container").show();
        } else {
            $("#raw_transaction_modal_transaction_bytes_container").hide();
        }

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
		$(".phasing_number_accounts_group").find("input[name=phasingQuorum]").val(1);

		var type = $modal.data('transactionType');
		var subType = $modal.data('transactionSubtype');
		if (type != undefined && subType != undefined) {
			if (NRS.transactionTypes[type]["subTypes"][subType]["serverConstants"]["isPhasingSafe"] == true) {
				$modal.find('.phasing_safe_alert').hide();
			} else {
				$modal.find('.phasing_safe_alert').show();
			}
		}

		var context = {
			labelText: "Finish Height",
			labelI18n: "finish_height",
			helpI18n: "approve_transaction_finish_height_help",
			inputName: "phasingFinishHeight",
			initBlockHeight: NRS.lastBlockHeight + 7000,
			changeHeightBlocks: 500
		}
		var $elems = NRS.initModalUIElement($modal, '.phasing_finish_height_group', 'block_height_modal_ui_element', context);
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
				$mbGroup.find('option[value="2"], option[value="3"]').remove();
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

		var selectName = $modal.attr('id') == "hash_modal" ? "hashAlgorithm" : "phasingHashedSecretAlgorithm";
		var context = {
			labelText: "HASH ALGORITHM",
			labelI18n: "hash_algorithm",
			selectName: selectName
		}
		NRS.initModalUIElement($modal, '.hash_algorithm_model_group', 'hash_algorithm_model_modal_ui_element', context);

		_setMandatoryApproval($modal);
		_setApprovalFeeAddition($modal);
	}

	function _setApprovalFeeAddition($modal) {
		if (!$modal) {
			$modal = $('.modal:visible');
		}
		var feeAddition = $modal.find('.approve_tab_list li.active a').data("feeNxtApprovalAddition");
		var $mbSelect = $modal.find('.tab_pane_approve.active .approve_min_balance_model_group select');
		if($mbSelect.length > 0 && $mbSelect.val() != "0") {
			feeAddition = String(20);
		}

        $modal.find("input[name='feeNXT_approval_addition']").val(feeAddition);
        $modal.find("span.feeNXT_approval_addition_info").html("+" + feeAddition);
	}

	function _setMandatoryApproval($modal) {
		$modal.one('shown.bs.modal', function (e) {
			if (NRS.accountInfo.accountControls && $.inArray('PHASING_ONLY', NRS.accountInfo.accountControls) > -1) {
				NRS.sendRequest("getPhasingOnlyControl", {
					"account": NRS.account
				}, function (response) {
					function completeFieldsSetup(asset, currency) {
						switch (response.phasingVotingModel) {
							case 0:
								$approveModal.find('.at_accounts').trigger('click');
								$approveModal.find('.tab-pane.active input[name="phasingQuorum"]').val(response.phasingQuorum);
								break;
							case 1:
								$approveModal.find('.at_balance').trigger('click');
								$approveModal.find('.tab-pane.active input[name="phasingQuorumNXT"]').val(NRS.convertToNXT(response.phasingQuorum));
								break;
							case 2:
								$approveModal.find('.at_asset_holders').trigger('click');
								$approveModal.find('.tab-pane.active input[name="phasingQuorumQNTf"]').val(NRS.convertToQNTf(response.phasingQuorum, asset.decimals));
								$approveModal.find('.tab-pane.active input[name="phasingHolding"]').val(response.phasingHolding);
								break;
							case 3:
								$approveModal.find('.at_currency_holders').trigger('click');
								$approveModal.find('.tab-pane.active input[name="phasingQuorumQNTf"]').val(NRS.convertToQNTf(response.phasingQuorum, currency.decimals));
								$approveModal.find('.tab-pane.active input[name="phasingHoldingCurrencyCode"]').val(currency.code);
								$approveModal.find('.tab-pane.active input[name="phasingHolding"]').val(response.phasingHolding);
								break;
						}

						$activeTabPane = $approveModal.find('.tab-pane.active');

						if (asset) {
							var nameString = String(asset.name) + "&nbsp; (" + $.t('decimals', 'Decimals') + ": " + String(asset.decimals) + ")";
							$activeTabPane.find('.aam_ue_asset_name').html(nameString);
							$activeTabPane.find('.aam_ue_asset_decimals_input').val(String(asset.decimals));
							$activeTabPane.find('.aam_ue_asset_decimals_input').prop("disabled", false);
						}

						if (currency) {
							var idString = String(currency.currency) + "&nbsp; (" + $.t('decimals', 'Decimals') + ": " + String(currency.decimals) + ")";
							$activeTabPane.find('.acm_ue_currency_id').html(idString);
							$activeTabPane.find('.acm_ue_currency_id_input').val(String(currency.currency));
							$activeTabPane.find('.acm_ue_currency_id_input').prop("disabled", false);
							$activeTabPane.find('.acm_ue_currency_decimals').html(String(currency.decimals));
							$activeTabPane.find('.acm_ue_currency_decimals_input').val(String(currency.decimals));
							$activeTabPane.find('.acm_ue_currency_decimals_input').prop("disabled", false);
						}

						if (response.phasingWhitelist && response.phasingWhitelist.length > 0) {
							for (var i = 0; i < response.phasingWhitelist.length - 1 && $activeTabPane.find('input[name="phasingWhitelisted"]').length < response.phasingWhitelist.length; i++) {
								//add empty fields for the whitelisted accounts if necessary
								$activeTabPane.find('.add_account_btn').trigger('click');
							}

							//fill the fields
							$activeTabPane.find('input[name="phasingWhitelisted"]').each(function (index, elem) {
								if (index < response.phasingWhitelist.length) {
									$(elem).val(NRS.convertNumericToRSAccountFormat(response.phasingWhitelist[index]));
									//$(elem).trigger('show');
								}
							});
						}

						var $mbSelect = $('.modal .approve_modal .approve_min_balance_model_group:visible select');
						$mbSelect.val(parseInt(response.phasingMinBalanceModel));

						switch (response.phasingMinBalanceModel) {
							case 1:
								$approveModal.find('.tab-pane.active input[name="phasingMinBalanceNXT"]').val(NRS.convertToNXT(response.phasingMinBalance));
								break;
							case 2:
								$approveModal.find('.tab-pane.active input[name="phasingMinBalanceQNTf"]').val(NRS.convertToQNTf(response.phasingMinBalance, asset.decimals));
								$approveModal.find('.tab-pane.active input[name="phasingHolding"]').val(response.phasingHolding);
								break;
							case 3:
								$approveModal.find('.tab-pane.active input[name="phasingMinBalanceQNTf"]').val(NRS.convertToQNTf(response.phasingMinBalance, currency.decimals));
								$approveModal.find('.tab-pane.active input[name="phasingHoldingCurrencyCode"]').val(currency.code);
								$approveModal.find('.tab-pane.active input[name="phasingHolding"]').val(response.phasingHolding);
								break;
						}
						$mbSelect.trigger('change');

						// Close accidentally triggered popovers
						$(".show_popover").popover("hide");
					}

					if (response && response.phasingVotingModel >= 0) {

						$modal.find('.phasing_safe_alert').hide();
						$modal.find('.phasing_only_enabled_info').show();
						$modal.find(".advanced").show(); //show the advanced stuff by default
						$modal.find(".advanced_info").hide(); //hide the button for switching to basic view

						var $approveModal = $modal.find(".approve_modal");

						if (response.phasingVotingModel == 2 || response.phasingMinBalanceModel == 2) {
							NRS.sendRequest("getAsset", {
								"asset": response.phasingHolding
							}, function (phResponse) {
								if (phResponse && phResponse.asset) {
									completeFieldsSetup(phResponse);
								}
							});
						} else if (response.phasingVotingModel == 3 || response.phasingMinBalanceModel == 3) {
							NRS.sendRequest("getCurrency", {
								"currency": response.phasingHolding
							}, function(phResponse) {
								if (phResponse && phResponse.currency) {
									completeFieldsSetup(null, phResponse);
								}
							});
						} else {
							completeFieldsSetup(null, null);
						}
					} else {

					}
				});
			}
		});
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