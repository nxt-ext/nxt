#!/bin/bash
VERSION=$1
if [ -x ${VERSION} ];
then
	echo VERSION not defined
	exit 1
fi
PACKAGE=nxt-client-${VERSION}
echo PACKAGE="${PACKAGE}"
CHANGELOG=nxt-client-${VERSION}.changelog.txt
OBFUSCATE=$2

FILES="changelogs conf html lib resource contrib"
FILES="${FILES} nxt.exe nxtservice.exe"
FILES="${FILES} 3RD-PARTY-LICENSES.txt AUTHORS.txt COPYING.txt DEVELOPER-AGREEMENT.txt LICENSE.txt"
FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh compact.sh compact.bat sign.sh"
FILES="${FILES} NXT_Wallet.url Dockerfile"

unix2dos *.bat
echo compile
./compile.sh
rm -rf html/doc/*
rm -rf nxt
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p nxt/
mkdir -p nxt/logs

if [ "${OBFUSCATE}" == "obfuscate" ]; 
then
echo obfuscate
/opt/proguard/bin/proguard.sh @nxt.pro
mv ../nxt.map ../nxt.map.${VERSION}
else
FILES="${FILES} classes src"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"
echo javadoc
./javadoc.sh
fi
echo copy resources
cp installer/lib/JavaExe.exe nxt.exe
cp installer/lib/JavaExe.exe nxtservice.exe
cp -a ${FILES} nxt
echo gzip
for f in `find nxt/html -name *.html -o -name *.js -o -name *.css -o -name *.json  -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9c "$f" > "$f".gz
done
cd nxt
echo generate jar files
../jar.sh
echo package installer Jar
../installer/build-installer.sh ../${PACKAGE}
#echo create installer exe
#../installer/build-exe.bat ${PACKAGE}
echo create installer zip
cd -
zip -q -X -r ${PACKAGE}.zip nxt -x \*/.idea/\* \*/.gitignore \*/.git/\* \*/\*.log \*.iml nxt/conf/nxt.properties nxt/conf/logging.properties
rm -rf nxt

#echo signing zip package
#../jarsigner.sh ${PACKAGE}.zip

echo signing jar package
../jarsigner.sh ${PACKAGE}.jar

echo creating change log ${CHANGELOG}
echo -e "Release $1\n" > ${CHANGELOG}
echo -e "https://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.zip\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.zip >> ${CHANGELOG}

echo -e "\nhttps://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.jar\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.jar >> ${CHANGELOG}

echo -e "\nhttps://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.exe\n" >> ${CHANGELOG}
#echo -e "sha256:\n" >> ${CHANGELOG}
#sha256sum ${PACKAGE}.exe >> ${CHANGELOG}

echo -e "The exe and jar packages must have a digital signature by \"Stichting NXT\"." >> ${CHANGELOG}

if [ "${OBFUSCATE}" == "obfuscate" ];
then
echo -e "\n\nThis is a development release for testing only. Source code is not provided." >> ${CHANGELOG}
fi
echo -e "\n\nChange log:\n" >> ${CHANGELOG}

cat changelogs/${CHANGELOG} >> ${CHANGELOG}
echo >> ${CHANGELOG}

gpg --detach-sign --armour --sign-with 0x811D6940E1E4240C ${PACKAGE}.zip
gpg --detach-sign --armour --sign-with 0x811D6940E1E4240C ${PACKAGE}.jar
#gpg --detach-sign --armour --sign-with 0x811D6940E1E4240C ${PACKAGE}.exe

gpg --clearsign --sign-with 0x811D6940E1E4240C ${CHANGELOG}
rm -f ${CHANGELOG}
gpgv ${PACKAGE}.zip.asc ${PACKAGE}.zip
gpgv ${PACKAGE}.jar.asc ${PACKAGE}.jar
#gpgv ${PACKAGE}.exe.asc ${PACKAGE}.exe
gpgv ${CHANGELOG}.asc
sha256sum -c ${CHANGELOG}.asc
#jarsigner -verify ${PACKAGE}.zip
jarsigner -verify ${PACKAGE}.jar


