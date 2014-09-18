#!/bin/sh
CP=classes:lib/*:conf
SP=src/java/

/bin/rm -rf html/doc/*

javadoc -quiet -sourcepath $SP -classpath $CP -protected -splitindex -subpackages nxt -d html/doc/
