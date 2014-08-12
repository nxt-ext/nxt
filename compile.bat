set CP=conf/;classes/;lib/*
set SP=src/java/
set JAVA_HOME="c:\Program Files\Java\jdk1.7.0_03"
md classes

%JAVA_HOME%\bin\javac -sourcepath %SP% -classpath %CP% -d classes/ src/java/nxt/*.java

del nxt.jar 
%JAVA_HOME%\bin\jar cf nxt.jar -C classes .

echo "nxt.jar generated successfully"
