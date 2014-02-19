CP=webapps/root/WEB-INF/lib/*:lib/*:lib/*/*
SP=src/java/

/bin/rm -rf webapps/root/WEB-INF/classes/*
/bin/mkdir -p webapps/root/WEB-INF/classes
/bin/rm -rf webapps/root/doc/*

javac -sourcepath $SP -classpath $CP -d webapps/root/WEB-INF/classes/ src/java/nxt/*.java src/java/nxt/*/*.java
