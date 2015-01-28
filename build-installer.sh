#!/bin/sh
/bin/rm -f -r dist
mkdir dist

java -Xmx512m -cp installerlib/izpack-compiler-5.0.0-rc5.jar:installerlib/* com.izforge.izpack.compiler.bootstrap.CompilerLauncher setup.xml -o dist/setup.jar > build-installer.log 2>&1