@echo off
pushd dist
del /q setup.exe
..\installer\lib\izpack2exe\izpack2exe.py --file setup.jar --output setup.exe --with-7z=..\installer\lib\izpack2exe\7za.exe --no-upx --with-jdk=..\jre --name nxt > ..\build-exe.log 2>&1
popd
