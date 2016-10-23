#!/bin/sh
rm -rf wallet
cordova create wallet org.nxt.mobile.wallet "NXT Mobile Wallet" --template ../../html
cd wallet
rm -rf icons
rm -rf plugins
cp -a ../../icons icons
cp -a ../../plugins plugins
cordova platform add android
rm -rf platforms
cp -a ../../platforms platforms
cd ..
