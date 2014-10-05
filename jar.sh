#!/bin/sh
/bin/rm -f nxt.jar 
jar cf nxt.jar -C classes . || exit 1

echo "nxt.jar generated successfully"
