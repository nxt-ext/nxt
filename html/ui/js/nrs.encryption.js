var NRS = (function(NRS, $, undefined) {
	var _decryptionPassword;
	var _decryptedTransactions = {};
	var _encryptedNote = null;
	var _sharedKeys = {};

	var _hash = {
		init: SHA256_init,
		update: SHA256_write,
		getBytes: SHA256_finalize
	};

	NRS.generatePublicKey = function(secretPhrase) {
		return NRS.getPublicKey(converters.stringToHexString(secretPhrase));
	}

	NRS.getPublicKey = function(secretPhrase, isAccountNumber) {
		if (isAccountNumber) {
			var accountNumber = secretPhrase;
			var publicKey = "";

			//synchronous!
			NRS.sendRequest("getAccountPublicKey", {
				"account": accountNumber
			}, function(response) {
				if (!response.publicKey) {
					throw "Account does not have a public key.";
				} else {
					publicKey = response.publicKey;
				}
			}, false);

			return publicKey;
		} else {
			var secretPhraseBytes = converters.hexStringToByteArray(secretPhrase);
			var digest = simpleHash(secretPhraseBytes);
			return converters.byteArrayToHexString(curve25519.keygen(digest).p);
		}
	}

	NRS.getPrivateKey = function(secretPhrase) {
		SHA256_init();
		SHA256_write(converters.stringToByteArray(secretPhrase));
		return converters.shortArrayToHexString(curve25519_clamp(converters.byteArrayToShortArray(SHA256_finalize())));
	}

	NRS.getAccountId = function(secretPhrase) {
		var publicKey = NRS.getPublicKey(converters.stringToHexString(secretPhrase));

		/*	
		if (NRS.accountInfo && NRS.accountInfo.publicKey && publicKey != NRS.accountInfo.publicKey) {
			return -1;
		}
		*/

		var hex = converters.hexStringToByteArray(publicKey);

		_hash.init();
		_hash.update(hex);

		var account = _hash.getBytes();

		account = converters.byteArrayToHexString(account);

		var slice = (converters.hexStringToByteArray(account)).slice(0, 8);

		return byteArrayToBigInteger(slice).toString();
	}

	NRS.encryptNote = function(message, options, secretPhrase) {
		try {
			if (!options.sharedKey) {
				if (!options.privateKey) {
					if (!secretPhrase) {
						if (NRS.rememberPassword) {
							secretPhrase = NRS.password;
						} else {
							throw {
								"message": "Your password is required to encrypt this message.",
								"errorCode": 1
							};
						}
					}

					options.privateKey = converters.hexStringToByteArray(NRS.getPrivateKey(secretPhrase));
				}

				if (!options.publicKey) {
					if (!options.account) {
						throw {
							"message": "Account ID not specified.",
							"errorCode": 2
						};
					}
					options.publicKey = converters.hexStringToByteArray(NRS.getPublicKey(options.account, true));
				}
			}

			var encrypted = encryptData(converters.stringToByteArray(message), options);

			return {
				"message": converters.byteArrayToHexString(encrypted.data),
				"nonce": converters.byteArrayToHexString(encrypted.nonce)
			};
		} catch (err) {
			if (err.errorCode && err.errorCode < 3) {
				throw err;
			} else {
				throw {
					"message": "The message could not be encrypted.",
					"errorCode": 3
				};
			}
		}
	}

	NRS.decryptNote = function(message, options, secretPhrase) {
		try {
			if (!options.sharedKey) {
				if (!options.privateKey) {
					if (!secretPhrase) {
						if (NRS.rememberPassword) {
							secretPhrase = NRS.password;
						} else if (_decryptionPassword) {
							secretPhrase = _decryptionPassword;
						} else {
							throw {
								"message": "Your password is required to decrypt this message.",
								"errorCode": 1
							};
						}
					}

					options.privateKey = converters.hexStringToByteArray(NRS.getPrivateKey(secretPhrase));
				}

				if (!options.publicKey) {
					if (!options.account) {
						throw {
							"message": "Account ID not specified.",
							"errorCode": 2
						};
					}
					options.publicKey = converters.hexStringToByteArray(NRS.getPublicKey(options.account, true));
				}
			}

			options.nonce = converters.hexStringToByteArray(options.nonce);

			return decryptData(converters.hexStringToByteArray(message), options);
		} catch (err) {
			if (err.errorCode && err.errorCode < 3) {
				throw err;
			} else {
				throw {
					"message": "The message could not be decrypted.",
					"errorCode": 3
				};
			}
		}
	}

	NRS.getSharedKeyWithAccount = function(account) {
		try {
			if (account in _sharedKeys) {
				return _sharedKeys[account];
			}

			var secretPhrase;

			if (NRS.rememberPassword) {
				secretPhrase = NRS.password;
			} else if (_decryptionPassword) {
				secretPhrase = _decryptionPassword;
			} else {
				throw {
					"message": "Your password is required.",
					"errorCode": 3
				};
			}

			var privateKey = converters.hexStringToByteArray(NRS.getPrivateKey(secretPhrase));

			var publicKey = converters.hexStringToByteArray(NRS.getPublicKey(account, true));

			var sharedKey = getSharedKey(privateKey, publicKey);

			var sharedKeys = Object.keys(_sharedKeys);

			if (sharedKeys.length > 50) {
				delete _sharedKeys[sharedKeys[0]];
			}

			_sharedKeys[account] = sharedKey;
		} catch (err) {
			throw err;
		}
	}

	NRS.signBytes = function(message, secretPhrase) {
		var messageBytes = converters.hexStringToByteArray(message);
		var secretPhraseBytes = converters.hexStringToByteArray(secretPhrase);

		var digest = simpleHash(secretPhraseBytes);
		var s = curve25519.keygen(digest).s;

		var m = simpleHash(messageBytes);

		_hash.init();
		_hash.update(m);
		_hash.update(s);
		var x = _hash.getBytes();

		var y = curve25519.keygen(x).p;

		_hash.init();
		_hash.update(m);
		_hash.update(y);
		var h = _hash.getBytes();

		var v = curve25519.sign(h, x, s);

		return converters.byteArrayToHexString(v.concat(h));
	}

	NRS.verifyBytes = function(signature, message, publicKey) {
		var signatureBytes = converters.hexStringToByteArray(signature);
		var messageBytes = converters.hexStringToByteArray(message);
		var publicKeyBytes = converters.hexStringToByteArray(publicKey);
		var v = signatureBytes.slice(0, 32);
		var h = signatureBytes.slice(32);
		var y = curve25519.verify(v, h, publicKeyBytes);

		var m = simpleHash(messageBytes);

		_hash.init();
		_hash.update(m);
		_hash.update(y);
		var h2 = _hash.getBytes();

		return areByteArraysEqual(h, h2);
	}

	NRS.setDecryptionPassword = function(password) {
		_decryptionPassword = password;
	}

	NRS.addDecryptedTransaction = function(identifier, content) {
		if (!_decryptedTransactions[identifier]) {
			_decryptedTransactions[identifier] = content;
		}
	}

	NRS.tryToDecryptMessage = function(message) {
		if (_decryptedTransactions && _decryptedTransactions[message.transaction]) {
			return _decryptedTransactions[message.transaction].message;
		}

		try {
			var decoded = NRS.decryptNote(message.attachment.message, {
				"nonce": message.attachment.nonce,
				"account": (message.recipient == NRS.account ? message.sender : message.recipient)
			});

			return decoded;
		} catch (err) {
			throw err;
		}
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

		var accountId = NRS.getAccountId(password);
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

	NRS.decryptAllMessages = function(messages, password) {
		if (!password) {
			throw {
				"message": "Secret phrase is a required field.",
				"errorCode": 1
			};
		} else {
			var accountId = NRS.getAccountId(password);
			if (accountId != NRS.account) {
				throw {
					"message": "Incorrect secret phrase.",
					"errorCode": 2
				};
			}
		}

		try {
			for (var otherUser in messages) {
				for (var key in messages[otherUser]) {
					var message = messages[otherUser][key];

					if (message.type = 1 && message.subtype == 8) {
						if (!_decryptedTransactions[message.transaction]) {
							var decoded = NRS.decryptNote(message.attachment.message, {
								"nonce": message.attachment.nonce,
								"account": otherUser
							}, password);

							_decryptedTransactions[message.transaction] = {
								"message": decoded
							};
						}
					}
				}
			}
		} catch (err) {
			throw err;
		}
	}

	function simpleHash(message) {
		_hash.init();
		_hash.update(message);
		return _hash.getBytes();
	}

	function areByteArraysEqual(bytes1, bytes2) {
		if (bytes1.length !== bytes2.length)
			return false;

		for (var i = 0; i < bytes1.length; ++i) {
			if (bytes1[i] !== bytes2[i])
				return false;
		}

		return true;
	}

	function curve25519_clamp(curve) {
		curve[0] &= 0xFFF8;
		curve[15] &= 0x7FFF;
		curve[15] |= 0x4000;
		return curve;
	}

	function byteArrayToBigInteger(byteArray, startIndex) {
		var value = new BigInteger("0", 10);
		var temp1, temp2;
		for (var i = byteArray.length - 1; i >= 0; i--) {
			temp1 = value.multiply(new BigInteger("256", 10));
			temp2 = temp1.add(new BigInteger(byteArray[i].toString(10), 10));
			value = temp2;
		}

		return value;
	}

	function aesEncrypt(plaintext, options) {
		// CryptoJS likes WordArray parameters
		var text = converters.byteArrayToWordArray(plaintext);

		if (!options.sharedKey) {
			var sharedKey = getSharedKey(options.privateKey, options.publicKey);
		} else {
			var sharedKey = options.sharedKey.slice(0); //clone
		}

		for (var i = 0; i < 32; i++) {
			sharedKey[i] ^= options.nonce[i];
		}

		var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));

		var tmp = new Uint8Array(16);
		window.crypto.getRandomValues(tmp);

		var iv = converters.byteArrayToWordArray(tmp);
		var encrypted = CryptoJS.AES.encrypt(text, key, {
			iv: iv
		});

		var ivOut = converters.wordArrayToByteArray(encrypted.iv);

		var ciphertextOut = converters.wordArrayToByteArray(encrypted.ciphertext);

		return ivOut.concat(ciphertextOut);
	}

	function aesDecrypt(ivCiphertext, options) {
		if (ivCiphertext.length < 16 || ivCiphertext.length % 16 != 0) {
			throw {
				name: "invalid ciphertext"
			};
		}

		var iv = converters.byteArrayToWordArray(ivCiphertext.slice(0, 16));
		var ciphertext = converters.byteArrayToWordArray(ivCiphertext.slice(16));

		if (!options.sharedKey) {
			var sharedKey = getSharedKey(options.privateKey, options.publicKey);
		} else {
			var sharedKey = options.sharedKey.slice(0); //clone
		}


		for (var i = 0; i < 32; i++) {
			sharedKey[i] ^= options.nonce[i];
		}

		var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));

		var encrypted = CryptoJS.lib.CipherParams.create({
			ciphertext: ciphertext,
			iv: iv,
			key: key
		});

		var decrypted = CryptoJS.AES.decrypt(encrypted, key, {
			iv: iv
		});

		var plaintext = converters.wordArrayToByteArray(decrypted);

		return plaintext;
	}

	function encryptData(plaintext, options) {
		if (!options.sharedKey) {
			options.sharedKey = getSharedKey(options.privateKey, options.publicKey);
		}

		var compressedPlaintext = pako.gzip(new Uint8Array(plaintext));

		options.nonce = new Uint8Array(32);
		window.crypto.getRandomValues(options.nonce);

		var data = aesEncrypt(compressedPlaintext, options);

		return {
			"nonce": options.nonce,
			"data": data
		};
	}

	function decryptData(data, options) {
		if (!options.sharedKey) {
			options.sharedKey = getSharedKey(options.privateKey, options.publicKey);
		}

		var compressedPlaintext = aesDecrypt(data, options);

		var binData = new Uint8Array(compressedPlaintext);

		var data = pako.inflate(binData);

		return converters.byteArrayToString(data);
	}

	function getSharedKey(key1, key2) {
		return converters.shortArrayToByteArray(curve25519_(converters.byteArrayToShortArray(key1), converters.byteArrayToShortArray(key2), null));
	}

	return NRS;
}(NRS || {}, jQuery));