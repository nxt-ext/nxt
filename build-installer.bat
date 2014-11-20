rmdir /s /q dist
md dist

java -Xmx512m  -classpath installerlib/*  com.izforge.izpack.compiler.bootstrap.CompilerLauncher setup.xml -o dist\setup.jar > build-installer.log 2>&1