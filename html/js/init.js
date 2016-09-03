/******************************************************************************
 * Copyright © 2016 The Nxt Core Developers.                                  *
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
NRS = {}
NRS.mobile = {};
NRS.mobile.OPEN_API_PEERS = {};
NRS.mobile.CORS_PEERS = ["23.94.134.161", "62.75.159.113", "193.151.106.129", "78.63.207.76", "93.211.249.225", "78.94.2.74", "46.101.137.39", "192.157.241.212", "192.157.233.106", "178.79.128.235", "82.165.145.37", "69.163.40.132", "104.168.174.36", "23.92.52.49", "162.243.122.251", "84.75.67.109", "174.140.168.144", "69.163.47.173", "88.130.215.71", "78.244.125.22", "192.3.196.10", "178.150.207.53", "174.140.167.239", "145.132.7.244", "99.224.242.103", "87.223.188.105", "80.150.243.12", "80.150.243.13", "80.150.243.99", "80.150.243.11", "80.150.243.110", "80.150.243.98", "80.150.243.97", "80.150.243.96", "80.150.243.88", "80.150.243.9", "146.185.168.142", "62.138.2.47", "212.47.243.151", "46.105.211.5", "37.48.73.249", "163.172.214.102", "45.63.40.176", "108.61.190.121", "104.207.134.36", "109.172.57.106", "91.239.69.78", "37.187.96.196", "188.165.250.19", "84.118.252.110", "148.251.57.155", "46.101.106.97", "198.105.223.72", "91.121.193.26", "5.196.72.29", "5.135.181.38", "178.63.60.81", "144.76.3.50", "104.193.41.253", "188.166.161.39", "104.236.180.85", "81.163.43.45", "159.203.122.239", "45.63.68.207", "62.194.6.113", "198.46.193.111", "163.172.152.118", "136.243.249.132", "89.138.120.211"];
NRS.mobile.customPeer = "";
NRS.mobile.customPort = "";

NRS.secret = "";
NRS.pages = {};
NRS.settings = NRS.defaultSettings;

i18nGlobal = {};
i18nGlobal = $.i18n;

NRS.settings = {};
NRS.settings.language = {};
NRS.settings["language"] = "en";
NRS.settings['regional_format'] = "en-US";

NRS.mobile.userSettingFileLoad = function(fileEntry)
{
	   fileEntry.file(function(file) {
        var reader = new FileReader();

        reader.onloadend = function(e) {
			var peerData = JSON.parse(this.result);
			NRS.mobile.OPEN_API_PEERS = peerData.OPEN_API_PEERS;
			NRS.mobile.customPeer = peerData.customPeer;
			NRS.mobile.customPort = peerData.customPort;			
			if(NRS.mobile.customPeer != "")
			{
				NRS.server = NRS.mobile.customPeer + NRS.mobile.customPort;
			}
			else
			{
				NRS.mobile.setRandomPeer(NRS.mobile.OPEN_API_PEERS);
			}
        }

        reader.readAsText(file);
    });
}

NRS.mobile.setRandomPeer = function(peers) {
	var min = 0;
	var max = peers.length;
	var rand = Math.floor(Math.random() * (max - min)) + min;
	NRS.server = "http://" + peers[rand] + ":7876";
}

i18nGlobal.init({
	fallbackLng: NRS.settings["language"],
	fallbackOnEmpty: true,
	lowerCaseLng: true,
	detectLngFromLocalStorage: true,
	resGetPath: "build/locales/__lng__/translation.json",
	debug: false
}, function() {

});

NRS.settings["language"] = i18nGlobal.lng();//If different language is detected from local storage

moment.locale(NRS.settings["language"]);

//function call that just returns original string, this is just workaround to support already existing NXT core JS
$.growl = function(x) {
	return (x);
}