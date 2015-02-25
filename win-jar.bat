java -cp classes nxt.util.ManifestGenerator 

del nxt.jar
jar cfm nxt.jar resource/nxt.manifest.mf -C classes .
del nxtservice.jar
jar cfm nxtservice.jar resource/nxtservice.manifest.mf -C classes .

echo "jar files generated successfully"