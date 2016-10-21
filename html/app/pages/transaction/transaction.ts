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
import {LoadingController, ToastController, NavController} from 'ionic-angular';

declare var i18nGlobal;
declare var NRS;
declare var NxtAddress;
declare var moment;

@Component({
  templateUrl: 'build/pages/transaction/transaction.html'
})
export class TransactionPage {
  transactions:any;
  loading: any;
    
  constructor(private navController: NavController, private loadingCtrl: LoadingController, private toastCtrl: ToastController) {

  }

  onPageDidEnter() {
	  this.loading = this.loadingCtrl.create({
		  content: i18nGlobal.t("loading")
		});	
		this.loading.present();		
		NRS.sendRequest("getBlockchainTransactions", {
			"account": NRS.account,
			"type": 0,
			"firstIndex": 0,
			"lastIndex": 50
		}, this.processTransactions);
  }

  processTransactions = (response) => {
	this.loading.dismiss();
    this.transactions = [];
	if (response.transactions && response.transactions.length) {
		
		for (let i = 0; i < response.transactions.length; i++) {
			let transaction = response.transactions[i];

			let fromAdd = "";
			let toAddr = "";
			let addressSenderStr;
			let addressRecipientStr;
			
			let addressSender = new NxtAddress();
			if (addressSender.set(transaction.sender))
				addressSenderStr = addressSender.toString();

			let addressRecipient = new NxtAddress();
			if(addressRecipient.set(transaction.recipient))
				addressRecipientStr = addressRecipient.toString()
			
			if(addressRecipientStr != NRS.accountRS) {
				transaction.class = "to";
				transaction.addr = addressRecipientStr;
			}
			else {
				transaction.class = "from";
				transaction.addr = addressSenderStr;
			}
			
			transaction.prefix = (transaction.class == "from")? "+ " : "- ";
			transaction.amount = NRS.convertToNXT(transaction.amountNQT).split(".");
			if(transaction.amount[1] != undefined)
				transaction.amount[0] = transaction.amount[0] + ".";
			
			transaction.moment = moment(new Date(NRS.formatTimestamp(parseInt(transaction.timestamp)))).fromNow();

			this.transactions.push(transaction);
		}
	} else {
		this.showToast(response);
	}
  }
  
  failedTxt() {
	return i18nGlobal.t("error_server_connect");
  }
  
  showToast = (msg) => {
	  let displayMsg = msg.errorDescription;
	  if(msg.errorCode == -1)
	  {
		displayMsg = this.failedTxt();
	  }

	  let toast = this.toastCtrl.create({
		message: displayMsg,
		duration: 5000
	  });

	  toast.present();
  }
  
  transactionsTxt() {
	return i18nGlobal.t("transaction");
  }  
}
