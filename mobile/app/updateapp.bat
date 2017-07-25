cd wallet
rmdir /s /q www
xcopy /y/i/s ..\..\..\html\www www
xcopy /y/i/s ..\..\..\html\config.xml config.xml
cd ..