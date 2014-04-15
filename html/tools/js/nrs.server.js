var NRS = (function(NRS, $, undefined) {
	$(document).ajaxComplete(function(event, xhr, settings) {
		if (xhr._page && xhr.statusText != "abort") {
			var index = $.inArray(xhr, NRS.xhrPool);
			if (index > -1) {
				NRS.xhrPool.splice(index, 1);
			}
		}
	});

	NRS.abortOutstandingRequests = function(subPage) {
		$(NRS.xhrPool).each(function(id, xhr) {
			if (subPage) {
				if (xhr._subPage) {
					xhr.abort();
				}
			} else {
				xhr.abort();
			}
		});

		if (!subPage) {
			NRS.xhrPool = [];
		}
	}

	NRS.beforeSendRequest = function(xhr) {
		xhr._page = true;
		if (NRS.currentSubPage) {
			xhr._subPage = true;
		}
		NRS.xhrPool.push(xhr);
	}

	NRS.sendOutsideRequest = function(url, data, callback, async) {
		if ($.isFunction(data)) {
			async = callback;
			callback = data;
			data = {};
		} else {
			data = data || {};
		}

		$.support.cors = true;

		$.ajax({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: "GET",
			timeout: 15000,
			async: (async === undefined ? true : async),
			data: data
		}).done(function(json) {
			if (json.errorCode && !json.errorDescription) {
				json.errorDescription = (json.errorMessage ? json.errorMessage : "Unknown error occured.");
			}
			if (callback) {
				callback(json, data);
			}
		}).fail(function(xhr, textStatus, error) {
			if (callback) {
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	NRS.sendRequest = function(requestType, data, callback, async) {
		if (requestType == undefined) {
			return;
		}

		if ($.isFunction(data)) {
			async = callback;
			callback = data;
			data = {};
		} else {
			data = data || {};
		}

		$.each(data, function(key, val) {
			if (key != "secretPhrase") {
				if (typeof val == "string") {
					data[key] = $.trim(val);
				}
			}
		});

		var nxtFields = ["feeNxt", "amountNxt"];

		for (var i = 0; i < nxtFields.length; i++) {
			var nxtField = nxtFields[i];
			var field = nxtField.replace("Nxt", "");

			if (nxtField in data) {
				if (NRS.useNQT) {
					data[field + "NQT"] = NRS.convertToNQT(data[nxtField]);
				} else {
					data[field] = data[nxtField];
				}

				delete data[nxtField];
			}
		}

		//gets account id from secret phrase client side, used only for login.
		if (requestType == "getAccountId") {
			var accountId = NRS.generateAccountId(data.secretPhrase, true);

			if (callback) {
				callback({
					"accountId": accountId
				});
			}
			return;
		}

		//check to see if secretPhrase supplied matches logged in account, if not - show error.
		if ("secretPhrase" in data) {
			var accountId = NRS.generateAccountId(NRS.rememberPassword ? sessionStorage.getItem("secret") : data.secretPhrase);
			if (accountId != NRS.account) {
				if (callback) {
					callback({
						"errorCode": 1,
						"errorDescription": "Incorrect secret phrase."
					});
				}
				return;
			} else {
				//ok, accountId matches..continue with the real request.
				NRS.processAjaxRequest(requestType, data, callback, async);
			}
		} else {
			NRS.processAjaxRequest(requestType, data, callback, async);
		}
	}

	NRS.processAjaxRequest = function(requestType, data, callback, async) {
		if (data["_extra"]) {
			var extra = data["_extra"];
			delete data["_extra"];
		} else {
			var extra = null;
		}

		var beforeSend = null;

		//means it is a page request, not a global request.. Page requests can be aborted.
		if (requestType.slice(-1) == "+") {
			requestType = requestType.slice(0, -1);

			beforeSend = NRS.beforeSendRequest;
		} else {
			//not really necessary... we can just use the above code..
			var plusCharacter = requestType.indexOf("+");

			if (plusCharacter > 0) {
				var subType = requestType.substr(plusCharacter);
				requestType = requestType.substr(0, plusCharacter);
				beforeSend = NRS.beforeSendRequest;
			}
		}

		var type = ("secretPhrase" in data ? "POST" : "GET");
		var url = NRS.server + "/nxt?requestType=" + requestType;

		if (type == "GET") {
			if (typeof data == "string") {
				data += "&random=" + Math.random();
			} else {
				data.random = Math.random();
			}
		}

		var secretPhrase = "";

		if (!NRS.isLocalHost && type == "POST" && requestType != "startForging" && requestType != "stopForging") {
			if (NRS.rememberPassword) {
				secretPhrase = sessionStorage.getItem("secret");
			} else {
				secretPhrase = data.secretPhrase;
			}

			delete data.secretPhrase;

			if (NRS.accountBalance && NRS.accountBalance.publicKey) {
				data.publicKey = NRS.accountBalance.publicKey;
			} else {
				data.publicKey = NRS.generatePublicKey(secretPhrase);
			}
		} else if (type == "POST" && NRS.rememberPassword) {
			data.secretPhrase = sessionStorage.getItem("secret");
		}

		$.support.cors = true;

		$.ajax({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: type,
			timeout: 15000, //15 seconds
			async: (async === undefined ? true : async),
			beforeSend: beforeSend,
			data: data
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (secretPhrase && response.transactionBytes && !response.errorCode) {
				var publicKey = NRS.generatePublicKey(secretPhrase);
				var signature = nxtCrypto.sign(response.transactionBytes, converters.stringToHexString(secretPhrase));

				if (!nxtCrypto.verify(signature, response.transactionBytes, publicKey)) {
					if (callback) {
						callback({
							"errorCode": 1,
							"errorDescription": "Could not verify signature (client side)."
						}, data);
					} else {
						$.growl("Could not verify signature.", {
							"type": "danger"
						});
					}
					return;
				} else {
					var payload = response.transactionBytes.substr(0, 128) + signature + response.transactionBytes.substr(256);

					if (!NRS.verifyTransactionBytes(payload, requestType, data)) {
						if (callback) {
							callback({
								"errorCode": 1,
								"errorDescription": "Could not verify transaction bytes (server side)."
							}, data);
						} else {
							$.growl("Could not verify transaction bytes.", {
								"type": "danger"
							});
						}
						return;
					} else {
						if (callback) {
							if (extra) {
								data["_extra"] = extra;
							}

							NRS.broadcastTransactionBytes(payload, callback, response, data);
						} else {
							NRS.broadcastTransactionBytes(payload);
						}
					}
				}
			} else {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
				}

				if (callback) {
					if (extra) {
						data["_extra"] = extra;
					}
					callback(response, data);
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if ((error == "error" || textStatus == "error") && (xhr.status == 404 || xhr.status == 0)) {
				if (type == "POST") {
					$.growl("Could not connect.", {
						"type": "danger",
						"offset": 10
					});
				}
			}

			if (error == "abort") {
				return;
			} else if (callback) {
				if (error == "timeout") {
					error = "The request timed out. Warning: This does not mean the request did not go through. You should wait a couple of blocks and see if your request has been processed.";
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	NRS.verifyTransactionBytes = function(transactionBytes, requestType, data) {
		var transaction = {};

		var currentPosition = 0;

		var byteArray = converters.hexStringToByteArray(transactionBytes);

		transaction.type = byteArray[0];
		transaction.subType = byteArray[1];
		transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, 2));
		transaction.deadline = String(converters.byteArrayToSignedShort(byteArray, 6));
		//sender public key == bytes 8 - 39
		transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 40));
		transaction.amount = String(converters.byteArrayToSignedInt32(byteArray, 48));
		transaction.fee = String(converters.byteArrayToSignedInt32(byteArray, 52));
		transaction.referencedTransaction = String(converters.byteArrayToBigInteger(byteArray, 56));

		if (transaction.referencedTransaction == "0") {
			transaction.referencedTransaction = null;
		}

		//signature == 64 - 127

		if (!("amount" in data)) {
			data.amount = "0";
		}

		if (!("recipient" in data)) {
			//recipient == genesis
			data.recipient = "1739068987193023818";
		}

		if (transaction.deadline !== data.deadline || transaction.recipient !== data.recipient || transaction.amount !== data.amount || transaction.fee !== data.fee) {
			return false;
		}

		if ("referencedTransaction" in data && transaction.referencedTransaction !== data.referencedTransaction) {
			return false;
		}

		var pos = 128;

		switch (requestType) {
			case "sendMoney":
				if (transaction.type !== 0 || transaction.subType !== 0) {
					return false;
				}
				break;
			case "sendMessage":
				if (transaction.type !== 1 || transaction.subType !== 0) {
					return false;
				}

				var message_length = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				var slice = byteArray.slice(pos, pos + message_length);

				transaction.message = converters.byteArrayToHexString(slice);

				if (transaction.message !== data.message) {
					return false;
				}
				break;
			case "assignAlias":
				if (transaction.type !== 1 || transaction.subType !== 1) {
					return false;
				}

				var alias_length = parseInt(byteArray[pos], 10);

				pos++;

				transaction.alias = converters.byteArrayToString(byteArray, pos, alias_length);

				pos += alias_length;

				var uri_length = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.uri = converters.byteArrayToString(byteArray, pos, uri_length);

				if (transaction.alias !== data.alias || transaction.uri !== data.uri) {
					return false;
				}
				break;
			case "createPoll":
				if (transaction.type !== 1 || transaction.subType !== 2) {
					return false;
				}

				var name_length = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.name = converters.byteArrayToString(byteArray, pos, name_length);

				pos += name_length;

				var description_length = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, description_length);

				pos += description_length;

				var nr_options = byteArray[pos];

				pos++;

				for (var i = 0; i < nr_options; i++) {
					var option_length = converters.byteArrayToSignedShort(byteArray, pos);

					pos += 2;

					transaction["option" + i] = converters.byteArrayToString(byteArray, pos, option_length);

					pos += option_length;
				}

				transaction.minNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.maxNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.optionsAreBinary = String(byteArray[pos]);

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.minNumberOfOptions !== data.minNumberOfOptions || transaction.maxNumberOfOptions !== data.maxNumberOfOptions || transaction.optionsAreBinary !== data.optionsAreBinary) {
					return false;
				}

				for (var i = 0; i < nr_options; i++) {
					if (transaction["option" + i] !== data["option" + i]) {
						return false;
					}
				}

				if (("option" + i) in data) {
					return false;
				}

				break;
			case "castVote":
				if (transaction.type !== 1 || transaction.subType !== 3) {
					return false;
				}

				transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var vote_length = byteArray[pos];

				pos++;

				transaction.votes = [];

				for (var i = 0; i < vote_length; i++) {
					transaction.votes.push(bytesArray[pos]);

					pos++;
				}

				return false;
				break;
			case "issueAsset":
				if (transaction.type !== 2 || transaction.subType !== 0) {
					return false;
				}

				var name_length = byteArray[pos];

				pos++;

				transaction.name = converters.byteArrayToString(byteArray, pos, name_length);

				pos += name_length;

				var description_length = converters.byteArrayToSignedShort(byteArray, pos); //6-7

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, description_length);

				pos += description_length;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.quantity !== data.quantity) {
					return false;
				}
				break;
			case "transferAsset":
				if (transaction.type !== 2 || transaction.subType !== 1) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				if (transaction.asset !== data.asset || transaction.quantity !== data.quantity) {
					return false;
				}
				break;
			case "placeAskOrder":
			case "placeBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "placeAskOrder" && transaction.subType !== 2) {
					return false;
				} else if (requestType == "placeBidOrder" && transaction.subType !== 3) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				transaction.price = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.asset !== data.asset || transaction.quantity !== data.quantity || transaction.price !== data.price) {
					return false;
				}
				break;
			case "cancelAskOrder":
			case "cancelBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "cancelAskOrder" && transaction.subType !== 4) {
					return false;
				} else if (requestType == "cancelBidOrder" && transaction.subType !== 5) {
					return false;
				}

				transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.order !== data.order) {
					return false;
				}

				break;
			default:
				//invalid requestType..
				return false;
		}

		return true;
	}

	NRS.broadcastTransactionBytes = function(transactionData, callback, original_response, original_data) {
		$.ajax({
			url: NRS.server + "/nxt?requestType=broadcastTransaction",
			crossDomain: true,
			dataType: "json",
			type: "POST",
			timeout: 20000, //20 seconds
			async: true,
			data: {
				"transactionBytes": transactionData
			}
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (callback) {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
					callback(response, original_data);
				} else {
					callback(original_response, original_data);
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if (callback) {
				if (error == "timeout") {
					error = "The request timed out. Warning: This does not mean the request did not go through. You should wait for the next block and see if your request has been processed.";
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	return NRS;
}(NRS || {}, jQuery));