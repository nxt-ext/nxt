#!/bin/sh
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
${JAVA} -Djava.security.manager -Djava.security.policy=nxt.policy -cp classes:lib/*:conf:addons/classes nxt.Nxt
