CP=webapps/root/WEB-INF/lib/*:lib/*:lib/*/*
SP=src/java/

javadoc -quiet -sourcepath $SP -classpath $CP -protected -splitindex -subpackages nxt -d webapps/root/doc/
