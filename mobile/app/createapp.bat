rmdir /s /q wallet
call cordova create wallet org.nxt.mobile.wallet "NXT Mobile Wallet" --template ..\..\html
cd wallet
xcopy /y/i/s ..\..\icons icons
call cordova platform add android
xcopy /y/i/s ..\..\platforms platforms
call cordova plugin add cordova-plugin-file
call cordova plugin add phonegap-plugin-barcodescanner
call cordova plugin add cordova-plugin-android-permissions
call cordova plugin add cordova-plugin-inappbrowser
call cordova plugin add cordova.plugins.diagnostic
cd ..