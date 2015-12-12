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
	var _tagsPerPage = 34;
	var _currentSearch = {
		"page": "",
		"searchStr": ""
	};

	NRS.getDataItemHTML = function(data) {
		var html = "";
		html += '<div id="' + data.transaction +'" style="border:1px solid #ccc;padding:12px;margin-top:12px;margin-bottom:12px;">';
		html += "<div style='float:right;color: #999999;background:white;padding:5px;border:1px solid #ccc;border-radius:3px'>" +
			"<strong>" + $.t("account") + "</strong>: <span>" + NRS.getAccountLink(data, "account") + "</span></div>" +
			"<h3 class='title'>" + String(data.name).escapeHTML() + "</h3>";

		var tags = data.parsedTags;
		for (var i=0; i<tags.length; i++) {
			html += '<span style="display:inline-block;background-color:#fff;padding:2px 5px 2px 5px;border:1px solid #f2f2f2;">';
			html += '<a href="#" class="tags" onclick="event.preventDefault(); NRS.tagged_data_search_tag(\'' + String(tags[i]).escapeHTML() + '\');">';
			html += String(tags[i]).escapeHTML() + '</a>';
			html += '</span>';
		}
		html += "</div>";
		html += '</div>';
		return html;
	};

	NRS.tagged_data_show_results = function(response) {
		var content = "";

		$("#tagged_data_search_contents").empty();
		$("#tagged_data_search_results").show();
		$("#tagged_data_search_center").hide();
		$("#tagged_data_search_top").show();

		if (response.data && response.data.length) {
			if (response.data.length > NRS.itemsPerPage) {
				NRS.hasMorePages = true;
				response.data.pop();
			} else {
				NRS.hasMorePages = false;
			}
			for (var i = 0; i < response.data.length; i++) {
				content += NRS.getDataItemHTML(response.data[i]);
			}
		}

		NRS.dataLoaded(content);
		NRS.showMore();
	};

	NRS.tagged_data_load_tags = function() {
		$('#tagged_data_tag_list').empty();
		NRS.sendRequest("getDataTags+", {
			"firstIndex": NRS.pageNumber * _tagsPerPage - _tagsPerPage,
			"lastIndex": NRS.pageNumber * _tagsPerPage
		}, function(response) {
			var content = "";
			if (response.tags && response.tags.length) {
				NRS.hasMorePages = response.tags.length > _tagsPerPage;
				for (var i=0; i<response.tags.length; i++) {
					content += '<div style="padding:5px 24px 5px 24px;text-align:center;background-color:#fff;font-size:16px;';
					content += 'width:220px;display:inline-block;margin:2px;border:1px solid #f2f2f2;">';
					content += '<a href="#" onclick="event.preventDefault(); NRS.tagged_data_search_tag(\'' +response.tags[i].tag + '\');">';
					content += response.tags[i].tag.escapeHTML() + ' [' + response.tags[i].count + ']</a>';
					content += '</div>';
				}
			}
			$('#tagged_data_tag_list').html(content);
			NRS.pageLoaded();
		});
	};

	NRS.tagged_data_search_account = function(account) {
		if (account == null) {
			account = _currentSearch["searchStr"];
		} else {
			_currentSearch = {
				"page": "account",
				"searchStr": account
			};
			NRS.pageNumber = 1;
			NRS.hasMorePages = false;
		}
		$(".tagged_data_search_pageheader_addon").hide();
		$(".tagged_data_search_pageheader_addon_account_text").text(account);
		$(".tagged_data_search_pageheader_addon_account").show();
		NRS.sendRequest("getAccountTaggedData+", {
			"account": account,
			"includeData": true,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			NRS.tagged_data_show_results(response);
		});
	};

	NRS.tagged_data_search_fulltext = function(query) {
		if (query == null) {
			query = _currentSearch["searchStr"];
		} else {
			_currentSearch = {
				"page": "fulltext",
				"searchStr": query
			};
			NRS.pageNumber = 1;
			NRS.hasMorePages = false;
		}
		$(".tagged_data_search_pageheader_addon").hide();
		$(".tagged_data_search_pageheader_addon_fulltext_text").text('"' + query + '"');
		$(".tagged_data_search_pageheader_addon_fulltext").show();
		NRS.sendRequest("searchTaggedData+", {
			"query": query,
            "includeData": true,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			NRS.tagged_data_show_results(response);
		});
	};

	NRS.tagged_data_search_tag = function(tag) {
		if (tag == null) {
			tag = _currentSearch["searchStr"];
		} else {
			_currentSearch = {
				"page": "tag",
				"searchStr": tag
			};
			NRS.pageNumber = 1;
			NRS.hasMorePages = false;
		}
		$(".tagged_data_search_pageheader_addon").hide();
		$(".tagged_data_search_pageheader_addon_tag_text").text('"' + tag + '"');
		$(".tagged_data_search_pageheader_addon_tag").show();
		NRS.sendRequest("searchTaggedData+", {
			"tag": tag,
            "includeData": true,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		}, function(response) {
			NRS.tagged_data_show_results(response);
		});
	};

	NRS.tagged_data_search_main = function(callback) {
		if (_currentSearch["page"] != "main") {
			NRS.pageNumber = 1;
			NRS.hasMorePages = false;
		}
		_currentSearch = {
			"page": "main",
			"searchStr": ""
		};
		$(".tagged_data_search input[name=q]").val("").trigger("unmask").mask("NXT-****-****-****-*****");
		$(".tagged_data_fulltext_search input[name=fs_q]").val("");
		$(".tagged_data_search_pageheader_addon").hide();
		$("#tagged_data_search_contents").empty();
		NRS.tagged_data_load_tags();

		$("#tagged_data_search_center").show();
		$("#tagged_data_search_top").hide();
		$("#tagged_data_search_results").hide();

		if (callback) {
			callback();
		}
	};

	NRS.pages.tagged_data_search = function(callback) {
		$("#tagged_data_top").show();
		$("#tagged_data_search_center").show();
		$("#tagged_data_pagination").show();
		if (_currentSearch["page"] == "account") {
			NRS.tagged_data_search_account();
		} else if (_currentSearch["page"] == "fulltext") {
			NRS.tagged_data_search_fulltext();
		} else if (_currentSearch["page"] == "tag") {
			NRS.tagged_data_search_tag();
		} else {
			NRS.tagged_data_search_main(callback);
		}
	};

	NRS.setup.tagged_data_search = function() {
		var sidebarId = 'sidebar_tagged_data';
		var options = {
			"id": sidebarId,
			"titleHTML": '<i class="fa fa-shopping-cart"></i><span data-i18n="tagged_data">Tagged Data</span>',
			"page": 'tagged_data_search',
			"desiredPosition": 60
		};
		NRS.addTreeviewSidebarMenuItem(options);
		options = {
			"titleHTML": '<span data-i18n="search">Search</span></a>',
			"type": 'PAGE',
			"page": 'tagged_data_search'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
	};

	$(".tagged_data_search").on("submit", function(e) {
		e.preventDefault();
		var account = $.trim($(this).find("input[name=q]").val());
		$(".tagged_data_search input[name=q]").val(account);

		if (account == "") {
			NRS.pages.tagged_data_search();
		} else if (/^(NXT\-)/i.test(account)) {
			var address = new NxtAddress();
			if (!address.set(account)) {
				$.growl($.t("error_invalid_account"), {
					"type": "danger"
				});
			} else {
				NRS.tagged_data_search_account(account);
			}
		} else {
            NRS.tagged_data_search_account(account);
		}
	});

	$(".tagged_data_fulltext_search").on("submit", function(e) {
		e.preventDefault();
		var query = $.trim($(this).find("input[name=fs_q]").val());
		if (query != "") {
			NRS.tagged_data_search_fulltext(query);
		}
	});

	$("#tagged_data_clear_results").on("click", function(e) {
		e.preventDefault();
		NRS.tagged_data_search_main();
	});

	return NRS;
}(NRS || {}, jQuery));