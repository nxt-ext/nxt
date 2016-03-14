#!/bin/sh
if [ -e ~/.nxt/nxt.pid ]; then
    PID=`cat ~/.nxt/nxt.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Nxt server already running"
        exit 1
    fi
fi
mkdir -p ~/.nxt/
REALPATH=`realpath "$0"`
DIR=`dirname "${REALPATH}"`
cd "${DIR}"
nohup java -cp classes:lib/*:conf -Dnxt.runtime.mode=desktop nxt.Nxt > /dev/null 2>&1 &
echo $! > ~/.nxt/nxt.pid
cd - > /dev/null
