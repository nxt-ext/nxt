#!/bin/sh
VERSION=$1
if [ -x ${VERSION} ];
then
	echo VERSION not defined
	exit 1
fi
PACKAGE=nxt-client-${VERSION}.zip
echo PACKAGE="${PACKAGE}"

FILES="changelogs classes conf html lib src resource"
FILES="${FILES} nxt.jar nxtservice.jar"
FILES="${FILES} 3RD-PARTY-LICENSES.txt AUTHORS.txt COPYING.txt DEVELOPER-AGREEMENT.txt LICENSE.txt"
FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh compact.sh compact.bat sign.sh"
FILES="${FILES} NXT_Wallet.url"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"

echo compile
./win-compile.sh
echo jar
./jar.sh
echo javadoc
rm -rf html/doc/*
./win-javadoc.sh

rm -rf nxt
rm -rf ${PACKAGE}
mkdir -p nxt/
mkdir -p nxt/logs
echo copy resources
cp -a ${FILES} nxt
echo gzip
for f in `find nxt/html -name *.gz`
do
	rm -f "$f"
done
for f in `find nxt/html -name *.html -o -name *.js -o -name *.css -o -name *.json -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9c "$f" > "$f".gz
done
echo zip
zip -q -X -r ${PACKAGE} nxt -x \*/.idea/\* \*/.gitignore \*/.git/\* \*/\*.log \*.iml nxt/conf/nxt.properties nxt/conf/logging.properties
rm -rf nxt
echo done
