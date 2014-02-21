CP=classes:lib/*
SP=src/java/

/bin/rm -rf html/doc/*

javadoc -quiet -sourcepath $SP -classpath $CP -protected -splitindex -subpackages nxt -d html/doc/
