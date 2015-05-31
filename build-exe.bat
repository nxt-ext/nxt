@echo off
del /q %1.exe
installerlib\izpack2exe\izpack2exe.py --file %1.jar --output %1.exe --with-7z=installerlib\izpack2exe\7za.exe --no-upx --with-jdk=jre --name nxt > build-exe.log 2>&1
