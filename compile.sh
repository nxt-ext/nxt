CP=classes/:lib/*
SP=src/java/

/bin/rm -rf classes/*

javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java
