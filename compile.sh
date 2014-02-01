/bin/rm -rf webapps/root/WEB-INF/classes/*
/usr/bin/javac -sourcepath src/java/ -classpath lib/*:lib/*/*:webapps/root/WEB-INF/lib/* -d webapps/root/WEB-INF/classes/ src/java/nxt/*.java src/java/nxt/*/*.java
