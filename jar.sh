#!/bin/sh
java -cp classes nxt.util.ManifestGenerator
/bin/rm -f nxt.jar
jar cfm nxt.jar src/META-INF/MANIFEST.MF -C classes . || exit 1
/bin/rm -f nxtservice.jar
jar cfm nxtservice.jar src/META-INF/MANIFEST.service.MF -C classes . || exit 1

echo "jar files generated successfully"