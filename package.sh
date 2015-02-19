#!/bin/sh
VERSION=$1
PACKAGE=nxt-client-${VERSION}.zip

FILES="conf lib html MIT-license.txt run.sh run.bat run-tor.sh verify.sh changelogs README.txt README_win.txt NXT_Wallet.url Dockerfile docker_start.sh classes nxt.jar src compile.sh win-compile.sh javadoc.sh package.sh mint.bat mint.sh"

./compile.sh
./jar.sh
rm -rf html/doc/*
./javadoc.sh

rm -rf nxt
rm -rf ${PACKAGE}
mkdir -p nxt/
cp -a ${FILES} nxt
for f in `find nxt/html -name *.html -o -name *.js -o -name *.css -o -name *.json -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9vc "$f" > "$f".gz
done
zip -X -r ${PACKAGE} nxt -x \*/.idea/\* \*/.gitignore \*/.git/\* \*.iml nxt/conf/nxt.properties nxt/conf/logging.properties
rm -rf nxt

