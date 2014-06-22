var NRS = (function(NRS, $, undefined) {
	var _decryptionPassword;
	var _decryptedTransactions = {};
	var _encryptedNote = null;

	NRS.unsetDecryptionPassword = function() {
		_decryptionPassword = "";
	}

	NRS.tryToDecrypt = function(transaction, fields, account, options) {
		var showDecryptionForm = false;

		if (!options) {
			options = {};
		}

		var nrFields = Object.keys(fields).length;

		var formEl = (options.formEl ? String(options.formEl).escapeHTML() : "#transaction_info_output_bottom");
		var outputEl = (options.outputEl ? String(options.outputEl).escapeHTML() : "#transaction_info_output_bottom");

		var output = "";

		var identifier = (options.identifier ? transaction[options.identifier] : transaction.transaction);

		//check in cache first..
		if (_decryptedTransactions && _decryptedTransactions[identifier]) {
			var decryptedTransaction = _decryptedTransactions[identifier];

			$.each(fields, function(key, title) {
				if (typeof title != "string") {
					title = title.title;
				}

				if (key in decryptedTransaction) {
					output += "<div" + (!options.noPadding ? " style='padding-left:5px'" : "") + ">" + (title ? "<label" + (nrFields > 1 ? " style='margin-top:5px'" : "") + ">" + String(title).toUpperCase().escapeHTML() + "</label>" : "") + "<div>" + String(decryptedTransaction[key]).escapeHTML().nl2br() + "</div></div>";
				} else {
					//if a specific key was not found, the cache is outdated..
					output = "";
					delete _decryptedTransactions[identifier];
					return false;
				}
			});
		}

		if (!output) {
			$.each(fields, function(key, title) {
				var data = "";

				var inAttachment = ("attachment" in transaction);

				var toDecrypt = (inAttachment ? transaction.attachment[key] : transaction[key]);

				if (toDecrypt) {
					if (typeof title != "string") {
						var nonce = (inAttachment ? transaction.attachment[title.nonce] : transaction[title.nonce]);
						title = title.title;
					} else {
						var nonce = (inAttachment ? transaction.attachment[key + "Nonce"] : transaction[key + "Nonce"]);
					}

					try {
						data = NRS.decryptNote(toDecrypt, {
							"nonce": nonce,
							"account": account
						});
					} catch (err) {
						var mesage = String(err.message ? err.message : err);
						if (err.errorCode && err.errorCode == 1) {
							showDecryptionForm = true;
							return false;
						} else {
							data = "Could not decrypt " + String(title).escapeHTML().toLowerCase() + ".";
						}
					}

					output += "<div" + (!options.noPadding ? " style='padding-left:5px'" : "") + ">" + (title ? "<label" + (nrFields > 1 ? " style='margin-top:5px'" : "") + ">" + String(title).toUpperCase().escapeHTML() + "</label>" : "") + "<div>" + String(data).escapeHTML().nl2br() + "</div></div>";
				}
			});
		}

		if (showDecryptionForm) {
			_encryptedNote = {
				"transaction": transaction,
				"fields": fields,
				"account": account,
				"options": options,
				"identifier": identifier
			};

			$("#decrypt_note_form_container").detach().appendTo(formEl);

			$("#decrypt_note_form_container, " + formEl).show();
		} else {
			NRS.removeDecryptionForm();
			$(outputEl).html(output).show();
		}
	}

	NRS.removeDecryptionForm = function($modal) {
		if (($modal && $modal.find("#decrypt_note_form_container").length) || (!$modal && $("#decrypt_note_form_container").length)) {
			$("#decrypt_note_form_container input").val("");
			$("#decrypt_note_form_container").hide().detach().appendTo("body");
		}
	}

	$("#transaction_info_modal").on("hide.bs.modal", function(e) {
		NRS.removeDecryptionForm($(this));
		$("#transaction_info_output_bottom, #transaction_info_output_top").html("").hide();
	});

	$("#decrypt_note_form_container button.btn-primary").click(function() {
		$("#decrypt_note_form_container").trigger("submit");
	});

	$("#decrypt_note_form_container").on("submit", function(e) {
		e.preventDefault();

		var $form = $(this);

		if (!_encryptedNote) {
			$form.find(".callout").html("Encrypted note not found.").show();
			return;
		}

		var password = $form.find("input[name=secretPhrase]").val();

		if (!password) {
			if (NRS.password) {
				password = NRS.password;
			} else if (_decryptionPassword) {
				password = _decryptionPassword;
			} else {
				$form.find(".callout").html("Secret phrase is a required field.").show();
				return;
			}
		}

		var accountId = NRS.generateAccountId(password);
		if (accountId != NRS.account) {
			$form.find(".callout").html("Incorrect secret phrase.").show();
			return;
		}

		var rememberPassword = $form.find("input[name=rememberPassword]").is(":checked");

		var otherAccount = _encryptedNote.account;

		var output = "";
		var decryptionError = false;
		var decryptedFields = {};

		var inAttachment = ("attachment" in _encryptedNote.transaction);

		var nrFields = Object.keys(_encryptedNote.fields).length;

		$.each(_encryptedNote.fields, function(key, title) {
			var note = (inAttachment ? _encryptedNote.transaction.attachment[key] : _encryptedNote.transaction[key]);

			if (typeof title != "string") {
				var noteNonce = (inAttachment ? _encryptedNote.transaction.attachment[title.nonce] : _encryptedNote.transaction[title.nonce]);
				title = title.title;
			} else {
				var noteNonce = (inAttachment ? _encryptedNote.transaction.attachment[key + "Nonce"] : _encryptedNote.transaction[key + "Nonce"]);
			}

			try {
				var note = NRS.decryptNote(note, {
					"nonce": noteNonce,
					"account": otherAccount
				}, password);

				decryptedFields[key] = note;

				output += "<div" + (!_encryptedNote.options.noPadding ? " style='padding-left:5px'" : "") + ">" + (title ? "<label" + (nrFields > 1 ? " style='margin-top:5px'" : "") + ">" + String(title).toUpperCase().escapeHTML() + "</label>" : "") + "<div>" + note.escapeHTML().nl2br() + "</div></div>";
			} catch (err) {
				decryptionError = true;
				var message = String(err.message ? err.message : err);

				$form.find(".callout").html(message.escapeHTML());
				return false;
			}
		});

		if (decryptionError) {
			return;
		}

		_decryptedTransactions[_encryptedNote.identifier] = decryptedFields;

		//only save 150 decryptions maximum in cache...
		var decryptionKeys = Object.keys(_decryptedTransactions);

		if (decryptionKeys.length > 150) {
			delete _decryptedTransactions[decryptionKeys[0]];
		}

		NRS.removeDecryptionForm();

		var outputEl = (_encryptedNote.options.outputEl ? String(_encryptedNote.options.outputEl).escapeHTML() : "#transaction_info_output_bottom");

		$(outputEl).html(output).show();

		_encryptedNote = null;

		if (rememberPassword) {
			_decryptionPassword = password;
		}
	});

	return NRS;
}(NRS || {}, jQuery));