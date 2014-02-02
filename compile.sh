CP=webapps/root/WEB-INF/lib/*:lib/*:lib/*/*
SP=src/java/

/bin/rm -rf webapps/root/WEB-INF/classes/*
/bin/rm -rf webapps/root/doc/*
/usr/bin/javac -sourcepath $SP -classpath $CP -d webapps/root/WEB-INF/classes/ src/java/nxt/*.java src/java/nxt/*/*.java
/usr/bin/javadoc -quiet -sourcepath $SP -classpath $CP -protected -splitindex -subpackages nxt -d webapps/root/doc/
