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
import {Component} from '@angular/core';
import {ToastController, Page, ViewController, ModalController, NavController} from 'ionic-angular';
import {TabsPage} from '../tabs/tabs';
declare var i18nGlobal;
declare var NRS;
declare var moment;
declare var PassPhraseGenerator;
declare var cordova;
declare var NxtAddress;

@Page({
    template: `
    <ion-header>
	<ion-navbar>
        <ion-title>{{genPassTxt()}}</ion-title>
		<ion-buttons end>
        <button nav-pop>
            <ion-icon [name]="'close'"></ion-icon>
        </button>
		</ion-buttons>
	</ion-navbar>
    </ion-header>
  <ion-content padding text-center>
  <div>{{autoGenTxt()}}</div>
  <ion-textarea [(ngModel)]="autoPassPhrase" class="txtarea-font"></ion-textarea>
  <div>{{warningTxt()}}</div>
  <div>{{memorizeTxt()}}</div>
    <button danger (click)="close()">{{closeTxt()}}</button>
  </ion-content>`
})
export class PassPhraseGeneratorModal {
	autoPassPhrase:any;
	
    constructor(private viewCtrl:ViewController) {
		PassPhraseGenerator.generatePassPhrase();
		this.autoPassPhrase = PassPhraseGenerator.passPhrase;
		PassPhraseGenerator.reset();
    }
	
	genPassTxt() {
		return i18nGlobal.t("generate") + " " + i18nGlobal.t("passphrase");
	}
	
	autoGenTxt() {
		return i18nGlobal.t("automatically_generated_passphrase_is");
	}
	
	warningTxt() {
		return i18nGlobal.t("passphrase_disclosure_warning");
	}
	
	memorizeTxt() {
		return i18nGlobal.t("memorize_passphrase_help");
	}
	
	closeTxt() {
		return i18nGlobal.t("close");
	}
	
    close() {
		this.viewCtrl.dismiss();
    }
}

@Component({
  templateUrl: 'build/pages/login/login.html'
})
export class LoginPage {
  loginData : string = "";
  nxtAddress : string = "";
  language : any;
  textType : string = "text";
  iconType : string = "ios-person-outline";
  rememberMe : boolean = true;

  constructor(private navController: NavController, private toastCtrl: ToastController, private modalCtrl: ModalController) {
  }
  
  onPageLoaded() {
	NRS.constants.EPOCH_BEGINNING = 1385294400000; //hardcoded genesis data
  }
  
  generatePassphrase() {
	let passPhraseGenModal = this.modalCtrl.create(PassPhraseGeneratorModal);
    passPhraseGenModal.present();
  }
  
  closeTxt() {
	return i18nGlobal.t("close");
  }
  
  switchInputType() {
	if(this.textType == "text") {
		this.textType = "password";
		this.iconType = "ios-key-outline";
	}
	else {
		this.textType = "text";
		this.iconType = "ios-person-outline";
	}
  }
  
  scanQRDone = (result) => {
	if(result.cancelled == false && result.format == "QR_CODE") {
	  if(this.textType == "text") {
		  let nxtAddress = new NxtAddress();
		  if (nxtAddress.set(result.text)) {
			this.loginData = this.nxtAddress = nxtAddress.toString();
		  }
		  else {
			this.showToast(i18nGlobal.t("recipient_malformed"), 'top');
		  }
		}
		else {
			this.loginData = result.text;
		}
	}
  }

  scanQRCode() {
  this.loginData = "";
  this.nxtAddress = "";
	try {
		cordova.plugins.barcodeScanner.scan( this.scanQRDone, 
			function (error) {
			}
		);
	}
	catch (e) {
	}
  }
  
  languageChanged() {
	for(var lng in NRS.languages) {
		if(NRS.languages[lng] == this.language) {
			NRS.settings["language"] = lng;
			i18nGlobal.setLng(NRS.settings["language"], null, function() {
					
				});
			moment.locale(NRS.settings["language"]);
			break;
		}
	}
  }
  
  defaultLang(lang) {
	if(lang == NRS.languages[NRS.settings["language"]])
		return true;
	else
		return false;
  }
  
  supportedLanguages() {
	let languages = [];
	for( var lg in NRS.languages) {
		languages.push(NRS.languages[lg]);
	}
	return languages;
  }

  getLang() {
	return (NRS.languages);
  }

  loginTxt() {
	if(this.textType == "text") {
		return "NXT-____-____-____-_____";
	}
	else {
		return i18nGlobal.t("passphrase");
	}
  }

  showToast = (msg, pos) => {
	  let toast = this.toastCtrl.create({
		message: msg,
		duration: 5000,
		position: pos
	  });

	  toast.present();
  }
  
  checkForInput() {
	let retVal = true;
	if((this.loginData == "") && this.textType == "password") {
		this.showToast(i18nGlobal.t("error_passphrase_required_login"), 'top');
		retVal = false;
	}
	else if(this.textType == "text") {
	  let nxtAddress = new NxtAddress();
	  if (nxtAddress.set(this.loginData)) {
		this.nxtAddress = nxtAddress.toString();
	  }
	  if(this.loginData == ""  || this.nxtAddress == "") {
		this.showToast(i18nGlobal.t("error_invalid_account_id"), 'top');
		retVal = false;
	  }
	}
	return retVal;
  }
  
  loginDataEntered() {
	if(this.checkForInput()) {
		if(this.textType == "text") {
			NRS.accountRS = this.loginData;
		}
		else {
			NRS.secret = this.loginData;
			this.loginData = "";
			if(NRS.secret.length < 35) {
				this.showToast(i18nGlobal.t("error_passphrase_length"), 'bottom');
			}
		}
		this.navController.push(TabsPage);
	}
  }
  
  switchToWebUI() {
	  if(this.checkForInput()) {
		let skynxt = <HTMLElement>document.querySelector('.skynxtApp');
		skynxt.style.visibility = "hidden";
		let nrs = <HTMLElement>document.querySelector('.nrsApp');
		nrs.style.visibility = "visible";
		let header = <HTMLElement>document.querySelector('.header')
		header.style.visibility = "visible";
		
		if(this.textType == "password") {
			NRS.login(true, this.loginData);
		}
		else {
			NRS.login(false, this.loginData);
		}
	  }
  }
}
