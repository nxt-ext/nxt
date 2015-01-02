#!/bin/sh
/bin/rm -f nxt.jar 
jar cfm nxt.jar src/META-INF/MANIFEST.MF -C classes . || exit 1

echo "nxt.jar generated successfully"
