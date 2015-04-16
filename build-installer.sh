#!/bin/sh
java -Xmx512m -cp "../installerlib/*" com.izforge.izpack.compiler.bootstrap.CompilerLauncher setup.xml -o $1.jar > build-installer.log 2>&1