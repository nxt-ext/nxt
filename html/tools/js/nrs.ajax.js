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

	return NRS;
}(NRS || {}, jQuery));