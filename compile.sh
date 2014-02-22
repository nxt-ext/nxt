CP=conf/:classes/:lib/*
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java

/bin/rm -f nxt.jar 
jar cvf nxt.jar -C classes .
/bin/rm -rf classes
