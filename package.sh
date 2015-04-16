#!/bin/bash
VERSION=$1
PACKAGE=nxt-client-setup-${VERSION}
echo PACKAGE="${PACKAGE}"
CHANGELOG=nxt-client-${VERSION}.changelog.txt

FILES="conf lib html resource MIT-license.txt run.sh run.bat run-tor.sh verify.sh changelogs README.txt README_win.txt NXT_Wallet.url Dockerfile docker_start.sh mint.bat mint.sh"
FILES="${FILES}  build-installer.sh setup.xml shortcutSpec.xml RegistrySpec.xml nxt.exe nxtservice.exe compile.sh win-compile.sh javadoc.sh"

# unix2dos *.bat
echo compile
./compile.sh
rm -rf html/doc/*
rm -rf nxt
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p nxt/

if [ $2 == "obfuscate" ]; 
then
echo obfuscate
/cygdrive/c/ltc/proguard5.2.1/bin/proguard.bat @nxt.pro
mv ../nxt.map ../nxt.map.${VERSION}
mkdir -p nxt/src/
else
FILES="${FILES} classes src compile.sh win-compile.sh javadoc.sh package.sh jar.sh build-installer.sh"
echo javadoc
./javadoc.sh
fi
echo copy resources
cp -a ${FILES} nxt
echo gzip
for f in `find nxt/html -name *.html -o -name *.js -o -name *.css -o -name *.json  -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9vc "$f" > "$f".gz
done
cd nxt
echo jar
../jar.sh
echo package installer Jar
./build-installer.sh ../${PACKAGE}
cd -
echo create installer exe
./build-exe.sh ${PACKAGE}
echo create installer zip
zip -X -r ${PACKAGE}.zip nxt -x \*/.idea/\* \*/.gitignore \*/.git/\* \*.iml nxt/conf/nxt.properties nxt/conf/logging.properties
# rm -rf nxt 

echo creating change log ${CHANGELOG}
echo -e "Release $1\n" > ${CHANGELOG}
echo -e "https://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.exe\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.exe >> ${CHANGELOG}
if [ $2 == "obfuscate" ];
then
echo -e "\n\nThis is a development release for testing only. Source code is not provided." >> ${CHANGELOG}
fi
echo -e "\n\nChange log:\n" >> ${CHANGELOG}

cat changelogs/${CHANGELOG} >> ${CHANGELOG}
echo >> ${CHANGELOG}
# echo sign package ${PACKAGE}
# gpg --detach-sign --armour --sign-with lyaffe ${PACKAGE}
# echo sign change log ${CHANGELOG}
# gpg --clearsign --sign-with l ${CHANGELOG}
# rm -f ${CHANGELOG}
# echo verify signatures
# gpgv ${PACKAGE}.asc
# gpgv ${CHANGELOG}.asc
# sha256sum -c ${CHANGELOG}.asc


