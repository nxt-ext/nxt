rmdir /s /q wallet
call cordova create wallet org.nxt.mobile.wallet "NXT Mobile Wallet" --template ..\..\html
cd wallet
xcopy /y/i/s ..\..\icons icons
xcopy /y/i/s ..\..\plugins plugins
call cordova platform add android
xcopy /y/i/s ..\..\platforms platforms
cd ..