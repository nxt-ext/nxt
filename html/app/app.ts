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
import {NavController, Platform, ionicBootstrap} from 'ionic-angular';
import {StatusBar} from 'ionic-native';
import {LoginPage} from './pages/login/login';
import {SendPage} from './pages/send/send';

declare var cordova;
declare var NRS;

@Component({
  template: '<ion-nav [root]="rootPage"></ion-nav>'
})
export class MyApp {
  private rootPage: any;
  private backPressed: Boolean = false;

  constructor(private platform: Platform) {
    this.rootPage = LoginPage;

    platform.ready().then(() => {	
		if(window.resolveLocalFileSystemURL)
		{
			window.resolveLocalFileSystemURL(cordova.file.applicationDirectory + "www/SkyNxt/user/skynxt.user", NRS.mobile.userSettingFileLoad, null);	
		}
		else
		{
			NRS.mobile.setRandomPeer(NRS.mobile.CORS_PEERS);
		}
		StatusBar.styleDefault();
	  
    });
  }

}

ionicBootstrap(MyApp);
