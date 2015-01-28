del nxt.jar 
jar cfm nxt.jar src/META-INF/MANIFEST.MF -C classes .
del nxtservice.jar
jar cfm nxtservice.jar src/META-INF/MANIFEST.service.MF -C classes .

echo "nxt.jar generated successfully"
