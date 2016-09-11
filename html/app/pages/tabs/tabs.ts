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
import {NavController} from 'ionic-angular';
import {SendPage} from '../send/send';
import {TransactionPage} from '../transaction/transaction';
import {LoginPage} from '../login/login';

declare var i18nGlobal;
declare var NRS;

@Component({
  templateUrl: 'build/pages/tabs/tabs.html'
})
export class TabsPage {

  private tab1Root: any;
  private tab2Root: any;
  sky_nxt_tabs : any = false;

  constructor(private navController: NavController) {
    // this tells the tabs component which Pages
    // should be each tab's root Page
    this.tab1Root = SendPage;
    this.tab2Root = TransactionPage;
  }
  
  onPageDidLeave() {
	this.sky_nxt_tabs = true;
  }
  
  logoff() {
	NRS.secret = "";
	NRS.account = "";
	NRS.accountRS = "";
	NRS.accountInfo = {};
	this.navController.setRoot(LoginPage);
  }
  
  transactionsTxt() {
	return i18nGlobal.t("transaction");
  }
  
  tabsendNxtTxt() {
	return i18nGlobal.t("send_nxt");
  }
}
