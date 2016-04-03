#!/bin/sh
CP="lib/*:classes"
SP=src/java/

/bin/rm -f nxt.jar
/bin/rm -f nxtservice.jar
/bin/rm -rf classes
/bin/mkdir -p classes/
/bin/rm -rf addons/classes
/bin/mkdir -p addons/classes/

find src/java/nxt/ -name "*.java" > sources.tmp
javac -encoding utf8 -sourcepath "${SP}" -classpath "${CP}" -d classes/ @sources.tmp || exit 1
rm -f sources.tmp

echo "nxt class files compiled successfully"

find addons/src/ -name "*.java" > addons.tmp
if [ -s addons.tmp ]
then
    javac -encoding utf8 -sourcepath "${SP}" -classpath "${CP}" -d addons/classes @addons.tmp || exit 1
    rm -f addons.tmp
else
    rm -f addons.tmp
    exit 0
fi

echo "addon class files compiled successfully"
