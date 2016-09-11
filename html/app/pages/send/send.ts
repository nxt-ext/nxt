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
import {Page, ViewController, AlertController, ModalController, LoadingController, ToastController, NavController} from 'ionic-angular';
import {LoginPage} from '../login/login';

declare var i18nGlobal;
declare var NxtAddress;
declare var NRS;
declare var converters;
declare var qrcode;
declare var cordova;

@Page({
    template: `
    <ion-header>
	<ion-navbar>
        <ion-title>{{accountQRCodeTxt()}}</ion-title>
		<ion-buttons end>
        <button nav-pop>
            <ion-icon [name]="'close'"></ion-icon>
        </button>
		</ion-buttons>
	</ion-navbar>
    </ion-header>
  <ion-content padding text-center>
  <strong>{{accountRS()}}</strong>
	<div [innerHTML]="qrCode">
	</div>
    <button (click)="close()">{{closeTxt()}}</button>
  </ion-content>`
})
export class AccountQRCodeModal {
	qrCode:any;
	
    constructor(private nav:NavController, private viewCtrl:ViewController) {
		var qr = qrcode(3, 'M');
		var text = NRS.accountRS.replace(/^[\s\u3000]+|[\s\u3000]+$/g, '');
		qr.addData(text);
		qr.make();
		this.qrCode = qr.createImgTag(6);	
    }
	
	accountQRCodeTxt() {
		return i18nGlobal.t("account_qr_code");
	}
	
	accountRS() {
		return NRS.accountRS;
	}
	
	closeTxt() {
		return i18nGlobal.t("close");
	}
	
    close() {
		this.viewCtrl.dismiss();
    }
}

@Component({
  templateUrl: 'build/pages/send/send.html'
})
export class SendPage {
  
  balance : string = "";
  address : string = "";
  amount : string = "";
  accountRS = "";
  balance_disp_spin : any = false;
  loading : any;
  
  constructor(private navController: NavController, private toastCtrl: ToastController, private modalCtrl: ModalController, private loadingCtrl: LoadingController, public alertCtrl: AlertController) {

  }

  showToast = (msg) => {
	  let displayMsg = msg.errorDescription;
	  if(msg.broadcasted) {
		displayMsg = i18nGlobal.t("success_send_money");
		this.address = "";
		this.amount = "";
		this.balanceUpdate();
	  }
	  if(msg.errorCode == -1) {
		displayMsg = this.failedTxt();
	  }

	  let toast = this.toastCtrl.create({
		message: displayMsg,
		duration: 5000,
		position: 'bottom'
	  });

	  toast.present();
  }
  
  publicKeyCallBack = (response) => {
	NRS.accountInfo = {
		"publickey" : response.publicKey
	};
	this.balanceUpdate();
  }
  
  onPageLoaded() {
	  let nxtAddress = new NxtAddress();
	  if(NRS.secret == undefined) {
		  if (nxtAddress.set(NRS.accountRS)) {
			NRS.account = nxtAddress.account_id();
			this.accountRS = nxtAddress.toString();

			NRS.sendRequest("getAccountPublicKey", {
				"account": NRS.account
			}, this.publicKeyCallBack
			);
		  }
	  }
	  else {
		  NRS.account = NRS.getAccountId(NRS.secret);  
		  if (nxtAddress.set(NRS.account)) {
			NRS.accountRS = this.accountRS = nxtAddress.toString();
		  }
		  NRS.accountInfo = {
			"publickey" : NRS.getPublicKey(converters.stringToHexString(NRS.secret))
		  };
		  this.balanceUpdate();
	  }
  }
  
  onPageWillLeave() {
  }

  logoff() {
	NRS.secret = "";
	NRS.account = "";
	NRS.accountRS = "";
	NRS.accountInfo = {};
	this.navController.push(LoginPage);
  }
  
  balanceUpdate() {
  	  NRS.sendRequest("getAccount", {
			"account": NRS.account
		}, this.balanceCallBack);
  }
  
  showQRCode() {
	let qrModal = this.modalCtrl.create(AccountQRCodeModal);
	qrModal.present();
  }
  
  loadingTxt() {
	return i18nGlobal.t("loading_please_wait");
  }
  
  balanceCallBack = (response) => {
  	this.balance_disp_spin = true;
	if (!response.errorCode) {
		if (response.account != NRS.account || response.accountRS != NRS.accountRS) {
			response.errorDescription = i18nGlobal.t("error_account_id");
		}
		else {
			this.balance = NRS.formatAmount(response.unconfirmedBalanceNQT, false, true).split(".");
			if(this.balance[1] != undefined)
				this.balance[0] = this.balance[0] + ".";
		}
	}
	else {
		this.showToast(response);
	}
  }

  addressTxt() {
	return i18nGlobal.t("recipient_account");
  }

  accountTxt() {
	return i18nGlobal.t("account");
  }
  
  balanceTxt() {
	return i18nGlobal.t("balance");
  }
  
  amountTxt() {
	return i18nGlobal.t("amount");
  }
  
  sendNxtTxt() {
	return i18nGlobal.t("send_nxt");
  }

  failedTxt() {
	return i18nGlobal.t("error_server_connect");
  }
    
  keydownEvent(e) {
  		let charCode = !e.charCode ? e.which : e.charCode;

		if (NRS.isControlKey(charCode) || e.ctrlKey || e.metaKey) {
			return;
		}

		NRS.validateDecimals(8, charCode, this.amount, e);
  }
  
  scanQRDone = (result) => {
	if(result.cancelled == false && result.format == "QR_CODE") {
		this.address = result.text;
	}	
  }
  
  scanQR() {
	try {
		cordova.plugins.barcodeScanner.scan( this.scanQRDone, 
			function (error) {
			}
		);
	} 
	catch (e) {
	}
  }

  onSendNxt = (response) => {
	this.loading.dismiss();	
	this.showToast(response);
  }
  
	showInputPassphrase() {
		let prompt = this.alertCtrl.create({
		  title: i18nGlobal.t("passphrase"),
		  inputs: [
			{
			  name: 'alertPassphrase',
			  placeholder: i18nGlobal.t("passphrase")
			},
		  ],
		  buttons: [
			{
			  text: i18nGlobal.t("cancel"),
			  handler: data => {
				console.log('Cancel clicked');
				console.log(data);
			  }
			},
			{
			  text: i18nGlobal.t("submit"),
			  handler: data => {
				console.log('submit clicked');
				console.log(data);
			  }
			}
		  ]
		});
		prompt.present();
	}

  sendNxt() {
	  let msg = { errorCode: 1, errorDescription:""};
	  if(this.address == "" || this.amount == "") {	    
		msg.errorDescription = i18nGlobal.t("error_invalid_input");
		this.showToast(msg);
		return;
	  }
	  let recipientAccountRS = "";
	  let nxtAddress = new NxtAddress();
	  if (nxtAddress.set(this.address)) {
		recipientAccountRS = nxtAddress.toString();
	  }
	  else {
		msg.errorDescription = i18nGlobal.t("recipient_malformed");
		this.showToast(msg);
		return;
	  }
	  
	this.loading = this.loadingCtrl.create({
		  content: "",
		  duration: 5000
		});	
	this.loading.present();	
	if(NRS.secret == undefined)
	{
		//this.showInputPassphrase();
		return;
	}
	NRS.sendRequest("sendMoney", {
		"recipient": recipientAccountRS,
		"type": "POST",
		"amountNQT": NRS.convertToNQT(this.amount),
		"secretPhrase": NRS.secret,
		"deadline": "1440",
		"feeNQT": NRS.convertToNQT(1)
	}, this.onSendNxt);
  }  
}