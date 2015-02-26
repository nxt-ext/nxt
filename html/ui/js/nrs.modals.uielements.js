/**
 * @depends {3rdparty/jquery-2.1.0.js}
 */
var NRS = (function(NRS, $, undefined) {

	var _delay = (function(){
  		var timer = 0;
  		return function(callback, ms){
    		clearTimeout (timer);
    		timer = setTimeout(callback, ms);
  		};
	})();

	//block_height_modal_ui_element
	function _updateBlockHeightEstimates($bhmElem) {
			var $input = $bhmElem.find(' .bhm_ue_time_input');
			var blockHeight = $input.val();
			var output = "<i class='fa fa-clock-o'></i> ";
			if (blockHeight) {
				var blockDiff = blockHeight - NRS.lastBlockHeight;
				var diffSecs = blockDiff * NRS.averageBlockGenerationTime;
				output += moment().add(diffSecs, 'seconds').format('lll') + " ";

			} else {
				output += '-';
			}
			$bhmElem.find(".bhm_ue_time_estimate").html(output);
	}

	function _changeBlockHeightFromButton($btn, add) {
		var $bhmElem = $btn.closest('div[data-modal-ui-element="block_height_modal_ui_element"]');
		var numBlocks = parseInt($btn.data('bhmUeNumBlocks'));
		var $input = $bhmElem.find(' .bhm_ue_time_input');
		var blockHeight = parseInt($input.val());
		if(add) {
			$input.val(String(blockHeight + numBlocks));
		} else {
			$input.val(String(blockHeight - numBlocks));
		}
	}

	$("body").on("show.bs.modal", '.modal', function(e) {
		var $bhmElems = $(this).find('div[data-modal-ui-element="block_height_modal_ui_element"]');
		$bhmElems.each(function(key, bhmElem) {
			_updateBlockHeightEstimates($(bhmElem));
			$(bhmElem).find(".bhm_ue_current_block_height").html(String(NRS.lastBlockHeight));
		});
	});
	$('body').on('keyup', '.modal div[data-modal-ui-element="block_height_modal_ui_element"] .bhm_ue_time_input', function(e) {
		var $bhmElem = $(this).closest('div[data-modal-ui-element="block_height_modal_ui_element"]');
		_updateBlockHeightEstimates($bhmElem);
	});

	$('body').on('click', '.modal div[data-modal-ui-element="block_height_modal_ui_element"] .bhm_ue_reduce_height_btn', function(e) {
		var $bhmElem = $(this).closest('div[data-modal-ui-element="block_height_modal_ui_element"]');
		_changeBlockHeightFromButton($(this), false);
		_updateBlockHeightEstimates($bhmElem);
	});

	$('body').on('click', '.modal div[data-modal-ui-element="block_height_modal_ui_element"] .bhm_ue_add_height_btn', function(e) {
		var $bhmElem = $(this).closest('div[data-modal-ui-element="block_height_modal_ui_element"]');
		_changeBlockHeightFromButton($(this), true);
		_updateBlockHeightEstimates($bhmElem);
	});


	//add_currency_modal_ui_element
	_currencyCode = null;
	_acmElem = null;
	_setCurrencyInfoNotExisting = function() {
		$(_acmElem).find('.acm_ue_currency_id').html($.t('not_existing', 'Not existing'));
		$(_acmElem).find('.acm_ue_currency_id_input').val("");
		$(_acmElem).find('.acm_ue_currency_id_input').prop("disabled", true);
		$(_acmElem).find('.acm_ue_currency_decimals_input').val("");
		$(_acmElem).find('.acm_ue_currency_decimals_input').prop("disabled", true);
	}

	_loadCurrencyInfoForCode = function() {
		if (_currencyCode && _currencyCode.length >= 3) {
			NRS.sendRequest("getCurrency", {
				"code": _currencyCode
			}, function(response) {
				if (response && response.currency) {
					$(_acmElem).find('.acm_ue_currency_id').html(String(response.currency));
					$(_acmElem).find('.acm_ue_currency_id_input').val(String(response.currency));
					$(_acmElem).find('.acm_ue_currency_id_input').prop("disabled", false);
					$(_acmElem).find('.acm_ue_currency_decimals_input').val(String(response.decimals));
					$(_acmElem).find('.acm_ue_currency_decimals_input').prop("disabled", false);
				} else {
					_setCurrencyInfoNotExisting();
				}
			});
		} else {
			_setCurrencyInfoNotExisting();
		}
	}

	$('body').on('keyup', '.modal div[data-modal-ui-element="add_currency_modal_ui_element"] .acm_ue_currency_code_input', function(e) {
		_acmElem = $(this).closest('div[data-modal-ui-element="add_currency_modal_ui_element"]');
		_currencyCode = $(this).val();
		_delay(_loadCurrencyInfoForCode, 1000 );
	});



	return NRS;
}(NRS || {}, jQuery));