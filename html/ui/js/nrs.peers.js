/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.pages.peers = function() {
		NRS.sendRequest("getPeers+", {
			"active": "true",
			"includePeerInfo": "true"
		}, function(response) {
			if (response.peers && response.peers.length) {
				var rows = "";
				var uploaded = 0;
				var downloaded = 0;
				var connected = 0;
				var upToDate = 0;
				var activePeers = 0;
				
				for (var i = 0; i < response.peers.length; i++) {
					var peer = response.peers[i];

					if (!peer) {
						continue;
					}

					activePeers++;
					downloaded += peer.downloadedVolume;
					uploaded += peer.uploadedVolume;
					if (peer.state == 1) {
						connected++;
					}

					var versionToCompare = (!NRS.isTestNet ? NRS.normalVersion.versionNr : NRS.state.version);

					if (NRS.versionCompare(peer.version, versionToCompare) >= 0) {
						upToDate++;
					}

					rows += "<tr>";
					rows += "<td>";
					rows += (peer.state == 1 ? "<i class='fa fa-check-circle' style='color:#5cb85c' title='Connected'></i>" : "<i class='fa fa-times-circle' style='color:#f0ad4e' title='Disconnected'></i>");
					rows += "&nbsp;&nbsp;" + (peer.announcedAddress ? String(peer.announcedAddress).escapeHTML() : "No name") + "</td>";
					rows += "<td" + (peer.weight > 0 ? " style='font-weight:bold'" : "") + ">" + NRS.formatWeight(peer.weight) + "</td>";
					rows += "<td>" + NRS.formatVolume(peer.downloadedVolume) + "</td>";
					rows += "<td>" + NRS.formatVolume(peer.uploadedVolume) + "</td>";
					rows += "<td><span class='label label-" + (NRS.versionCompare(peer.version, versionToCompare) >= 0 ? "success" : "danger") + "'>";
					rows += (peer.application && peer.version ? String(peer.application).escapeHTML() + " " + String(peer.version).escapeHTML() : "?") + "</label></td>";
					rows += "<td>" + (peer.platform ? String(peer.platform).escapeHTML() : "?") + "</td>"

					rows += "<td style='text-align:right;'>";
					rows += "<a class='btn btn-xs btn-default' href='#' data-toggle='modal' ";
					rows += "data-target='#blacklist_peer_modal' " + (NRS.needsAdminPassword ? "disabled " : "");
					rows += "data-peer='" + String(peer.announcedAddress).escapeHTML() + "'>" + $.t("blacklist") + "</a>";
					rows += "</td>";
					rows += "</tr>";
				}

				$("#peers_uploaded_volume").html(NRS.formatVolume(uploaded)).removeClass("loading_dots");
				$("#peers_downloaded_volume").html(NRS.formatVolume(downloaded)).removeClass("loading_dots");
				$("#peers_connected").html(connected).removeClass("loading_dots");
				$("#peers_up_to_date").html(upToDate + '/' + activePeers).removeClass("loading_dots");

				NRS.dataLoaded(rows);
			} else {
				$("#peers_uploaded_volume, #peers_downloaded_volume, #peers_connected, #peers_up_to_date").html("0").removeClass("loading_dots");
				NRS.dataLoaded();
			}
		});
	}

	NRS.incoming.peers = function() {
		NRS.loadPage("peers");
	}
	
	NRS.forms.addPeerComplete = function(response, data) {
		$.growl($.t("success_add_peer"), {
			"type": "success"
		});
		NRS.loadPage("peers");
	}
	
	NRS.forms.blacklistPeerComplete = function(response, data) {
		$.growl($.t("success_blacklist_peer"), {
			"type": "success"
		});
		NRS.loadPage("peers");
	}
	
	$("#blacklist_peer_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var peerAddress = $invoker.data("peer");
		
		$("#blacklist_peer_address").html(peerAddress);
		$("#blacklist_peer_field_id").val(peerAddress);
	});
	
	return NRS;
}(NRS || {}, jQuery));