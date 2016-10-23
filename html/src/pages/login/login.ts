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
import {ToastController, /*Page,*/ ViewController, ModalController, NavController, LoadingController} from 'ionic-angular';
import {TabsPage} from '../tabs/tabs';

declare var i18nGlobal;
declare var NRS;
declare var moment;
declare var PassPhraseGenerator;
declare var cordova;
declare var NxtAddress;

/*@Page({
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
})*/
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
  templateUrl: 'login.html'
})
export class LoginPage {
  loginData : string = "";
  nxtAddress : string = "";
  language : string = "";
  textType : string = "text";
  iconType : string = "ios-person-outline";
  rememberMe : boolean = true;
  languages : any;
  saved_accounts : boolean = false;
  accounts : any;
  account : string = "";

  constructor(private navController: NavController, private toastCtrl: ToastController, private modalCtrl: ModalController, private loadingCtrl: LoadingController) {
	//NRS.removeItem("savedNxtAccounts");

	this.supportedLanguages();
	this.language = NRS.languages[NRS.settings["language"]];
	this.listAccounts();
  }
  
  onPageLoaded() {
	NRS.constants.EPOCH_BEGINNING = 1385294400000; //hardcoded genesis data
  }
  
  listAccounts() {
	let accountSaved = NRS.getStrItem("savedNxtAccounts");
	if(accountSaved && accountSaved != '' && this.textType == "text") {
		this.accounts = accountSaved.split(";");
		this.saved_accounts = (this.accounts.length > 0) ? true : false;
		this.loginData = this.account = this.accounts[0];
		this.accounts[this.accounts.length-1] = "other";
	}
  }
  
  accountChanged() {
	if(this.account == "other") {
		this.saved_accounts = false;
	}
	else {
		this.saved_accounts = true;
	}
  }
  
  removeAccount() {
	let listAccounts;
	  if(this.account != "other") {
			listAccounts = NRS.getStrItem("savedNxtAccounts").replace(this.loginData+';','');
			if (listAccounts == '') {
				NRS.removeItem('savedNxtAccounts');
				this.saved_accounts = false;
				this.loginData = "";
			} else {
				NRS.setStrItem("savedNxtAccounts", listAccounts);
				this.listAccounts();
			}
		}
  }
  
  generatePassphrase() {
	let passPhraseGenModal = this.modalCtrl.create(PassPhraseGeneratorModal);
    passPhraseGenModal.present();
  }
  
  removeTxt() {
	return i18nGlobal.t("remove");
  }
  
  closeTxt() {
	return i18nGlobal.t("close");
  }
  
  switchInputType() {
	this.loginData = "";
	if(this.textType == "text") {
		this.saved_accounts = false;
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

  supportedLanguages() {
	this.languages = [];
	if(this.languages.length == 0) {
		for( var lg in NRS.languages) {
			this.languages.push(NRS.languages[lg]);
		}
	}
	return this.languages;
  }

  getLang() {
	return (NRS.languages);
  }

  rememberTxt() {
	return i18nGlobal.t("remember");
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
	let loading = this.loadingCtrl.create({
		  content: "",
		  duration: 4000
		});	
	loading.present();
	NRS.rememberPassword = this.rememberMe;
	NRS.loadJSFiles(false);
	if(this.checkForInput()) {
		if(this.textType == "text") {
			NRS.accountRS = this.loginData;
			NRS.isPassphraseLogin = false;
			if(this.rememberMe) { //this also remembers account
				let accountExists = 0;
				if (NRS.getStrItem("savedNxtAccounts")) {
					let accounts = NRS.getStrItem("savedNxtAccounts").split(";");
					for(let i = 0; i < accounts.length; i++) {
						if (accounts[i] == NRS.accountRS) {
							accountExists = 1;
						}
					}
				}
				if(!accountExists) {
					if (NRS.getStrItem("savedNxtAccounts") && NRS.getStrItem("savedNxtAccounts") != "") {
						let accounts = NRS.getStrItem("savedNxtAccounts") + NRS.accountRS + ";";
						NRS.setStrItem("savedNxtAccounts", accounts);
					} else {
						NRS.setStrItem("savedNxtAccounts", NRS.accountRS + ";");
					}
				}
			}
		}
		else {
			NRS.secret = this.loginData;
			NRS.isPassphraseLogin = true;
			this.loginData = "";
			if(NRS.secret.length < 35) {
				this.showToast(i18nGlobal.t("error_passphrase_length"), 'bottom');
			}
		}
		NRS.loginData = this.loginData;
		this.loginData = "";
		this.navController.push(TabsPage);
	}
  }
  
  switchToWebUI() {
	  //if(this.checkForInput()) {
		let skynxt = <HTMLElement>document.querySelector('.skynxtApp');
		skynxt.style.visibility = "hidden";
		//let nrs = <HTMLElement>document.querySelector('.nrsApp');
		//nrs.style.visibility = "visible";
		NRS.initWebUI();
		NRS.loadJSFiles(true);
		/*if(this.textType == "password") {
			NRS.login(true, this.loginData);
		}
		else {
			NRS.login(false, this.loginData);
		}*/
	  //}
  }
}
