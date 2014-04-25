var NRS = (function(NRS, $, undefined) {
	NRS.loadContacts = function() {
		NRS.contacts = {};

		NRS.database.select("contacts", null, function(error, contacts) {
			if (contacts.length) {
				$.each(contacts, function(index, contact) {
					NRS.contacts[contact.accountId] = contact;
				});
			}
		});
	}

	NRS.pages.contacts = function() {
		NRS.pageLoading();

		if (!NRS.databaseSupport) {
			$("#contact_page_database_error").show();
			$("#contacts_table_container").hide();
			$("#add_contact_button").hide();
			NRS.pageLoaded();
			return;
		}

		$("#contacts_table_container").show();
		$("#contact_page_database_error").hide();

		NRS.database.select("contacts", null, function(error, contacts) {
			if (contacts.length) {
				var rows = "";

				contacts.sort(function(a, b) {
					if (a.name.toLowerCase() > b.name.toLowerCase()) {
						return 1;
					} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
						return -1;
					} else {
						return 0;
					}
				});

				$.each(contacts, function(index, contact) {
					var contactDescription = contact.description;

					if (contactDescription.length > 100) {
						contactDescription = contactDescription.substring(0, 100) + "...";
					} else if (!contactDescription) {
						contactDescription = "-";
					}

					rows += "<tr><td><a href='#' data-toggle='modal' data-target='#update_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>" + contact.name.escapeHTML() + "</a></td><td><a href='#' data-user='" + String(contact.accountId).escapeHTML() + "' class='user_info'>" + String(contact.accountId).escapeHTML() + "</a></td><td>" + (contact.email ? contact.email.escapeHTML() : "-") + "</td><td>" + contactDescription.escapeHTML() + "</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#send_money_modal' data-contact='" + String(contact.name).escapeHTML() + "'>Send Nxt</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#send_message_modal' data-contact='" + String(contact.name).escapeHTML() + "'>Message</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#delete_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>Delete</a></td></tr>";
				});

				$("#contacts_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#contacts_table"));

				NRS.pageLoaded();
			} else {
				$("#contacts_table tbody").empty();
				NRS.dataLoadFinished($("#contacts_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.forms.addContact = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (!data.name) {
			return {
				"error": "Contact name is a required field."
			};
		} else if (!data.account_id) {
			return {
				"error": "Account ID is a required field."
			};
		}

		if (/^\d+$/.test(data.name)) {
			return {
				"error": "Contact name must contain alphabetic characters."
			};
		}

		if (data.account_id.charAt(0) == '@') {
			var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
			if (convertedAccountId) {
				data.account_id = convertedAccountId;
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		}

		var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

		NRS.database.select("contacts", [{
			"accountId": data.account_id
		}], function(error, contacts) {
			if (contacts.length) {
				$modal.find(".error_message").html("A contact with this account ID already exists.").show();
				$btn.button("reset");
				$modal.modal("unlock");
			} else {
				NRS.database.insert("contacts", {
					name: data.name,
					email: data.email,
					accountId: data.account_id,
					description: data.description
				}, function(error) {
					NRS.contacts[data.account_id] = {
						name: data.name,
						email: data.email,
						accountId: data.account_id,
						description: data.description
					};

					$btn.button("reset");
					$modal.modal("unlock");
					$modal.modal("hide");
					$.growl("Contact added successfully.", {
						"type": "success"
					});

					if (NRS.currentPage == "contacts") {
						NRS.pages.contacts();
					} else if (NRS.currentPage == "messages" && NRS.selectedContext) {
						var heading = NRS.selectedContext.find("h4.list-group-item-heading");
						if (heading.length) {
							heading.html(data.name.escapeHTML());
						}
						NRS.selectedContext.data("context", "messages_sidebar_update_context");
					}
				});

				return {
					"stop": true
				};
			}
		});
	}

	$("#update_contact_modal").on('show.bs.modal', function(e) {
		var $invoker = $(e.relatedTarget);

		var contactId = parseInt($invoker.data("contact"), 10);

		if (!contactId && NRS.selectedContext) {
			var accountId = NRS.selectedContext.data("account");

			NRS.database.select("contacts", [{
				"accountId": accountId
			}], function(error, contact) {
				contact = contact[0];

				$("#update_contact_id").val(contact.id);
				$("#update_contact_name").val(contact.name);
				$("#update_contact_email").val(contact.email);
				$("#update_contact_account_id").val(contact.accountId);
				$("#update_contact_description").val(contact.description);
			});
		} else {
			$("#update_contact_id").val(contactId);

			NRS.database.select("contacts", [{
				"id": contactId
			}], function(error, contact) {
				contact = contact[0];

				$("#update_contact_name").val(contact.name);
				$("#update_contact_email").val(contact.email);
				$("#update_contact_account_id").val(contact.accountId);
				$("#update_contact_description").val(contact.description);
			});
		}
	});

	NRS.forms.updateContact = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		if (!data.name) {
			return {
				"error": "Contact name is a required field."
			};
		} else if (!data.account_id) {
			return {
				"error": "Account ID is a required field."
			};
		}

		if (data.account_id.charAt(0) == '@') {
			var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
			if (convertedAccountId) {
				data.account_id = convertedAccountId;
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		}

		var contactId = parseInt($("#update_contact_id").val(), 10);

		if (!contactId) {
			return {
				"error": "Invalid contact."
			};
		}

		var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");

		NRS.database.select("contacts", [{
			"accountId": data.account_id
		}], function(error, contacts) {
			if (contacts.length && contacts[0].id != contactId) {
				$modal.find(".error_message").html("A contact with this account ID already exists.").show();
				$btn.button("reset");
				$modal.modal("unlock");
			} else {
				NRS.database.update("contacts", {
					name: data.name,
					email: data.email,
					accountId: data.account_id,
					description: data.description
				}, [{
					"id": contactId
				}], function(error) {
					if (contacts.length && data.account_id != contacts[0].accountId) {
						delete NRS.contacts[contacts[0].accountId];
					}

					NRS.contacts[data.account_id] = {
						name: data.name,
						email: data.email,
						accountId: data.account_id,
						description: data.description
					};

					$btn.button("reset");
					$modal.modal("unlock");
					$modal.modal("hide");
					$.growl("Contact updated successfully.", {
						"type": "success"
					});

					if (NRS.currentPage == "contacts") {
						NRS.pages.contacts();
					} else if (NRS.currentPage == "messages" && NRS.selectedContext) {
						var heading = NRS.selectedContext.find("h4.list-group-item-heading");
						if (heading.length) {
							heading.html(data.name.escapeHTML());
						}
					}
				});

				return {
					"stop": true
				};
			}
		});
	}

	$("#delete_contact_modal").on('show.bs.modal', function(e) {
		var $invoker = $(e.relatedTarget);

		var contactId = $invoker.data("contact");

		$("#delete_contact_id").val(contactId);

		NRS.database.select("contacts", [{
			"id": contactId
		}], function(error, contact) {
			contact = contact[0];

			$("#delete_contact_name").html(contact.name.escapeHTML());
			$("#delete_contact_account_id").val(contact.accountId);
		});
	});

	NRS.forms.deleteContact = function($modal) {
		var id = parseInt($("#delete_contact_id").val(), 10);

		NRS.database.delete("contacts", [{
			"id": id
		}], function() {
			delete NRS.contacts[$("#delete_contact_account_id").val()];

			$.growl("Contact deleted successfully.", {
				"type": "success"
			});

			if (NRS.currentPage == "contacts") {
				NRS.pages.contacts();
			}
		});

		return {
			"stop": true
		};
	}

	return NRS;
}(NRS || {}, jQuery));