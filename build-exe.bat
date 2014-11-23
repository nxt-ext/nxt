@echo off
pushd dist
del /q setup.exe
..\installerlib\izpack2exe\izpack2exe.py --file setup.jar --output setup.exe --with-7z=..\installerlib\izpack2exe\7za.exe --no-upx --with-jdk=..\jre --name nxt > build-exe.log 2>&1
popd
