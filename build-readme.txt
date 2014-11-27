Building the NXT installer

Note: all pathes are relative to the git repository folder (nxt-private)

Pre-requisites for both Unix and Windows
========================================
Compile classes using compile.sh/compile.bat
Package NXT.jar using jar.sh/jar.bat 
mkdir jre

Building the installer on Unix
==============================
Execute the build: ./build-installer.sh
Review results: vi build-installer.log
Test X11 installer: java -jar ./dist/setup.jar
Test console installer: java -jar ./dist/setup.jar -console

Building the installer on Windows
=================================
Initial setup (perform once for each development workstation)
Download and install the latest version of the Java X86 JDK (32 bit) on your local workstation
Copy the content of Java JRE folder (C:\Program Files (x86)\Java\jre7) into the jre folder under your nxt-private git repository preserving the folder structure
Copy the server VM files from the JDK installation jre\bin\server folder to bin\server folder of the jre as explained in the the official Java readme file
Install python and associate it with .py file extension

Execute the build: build-installer.bat
Review results: notepad build-installer.log
Build executable: build-exe.bat
Review results: notepad build-exe.log
Test the installer using Java: java -jar dist/setup.jar
Test the windows executable: java -jar dist/setup.exe

Technical information
=====================
Installer is based on the IzPack product from http://izpack.org/
Installer project source code is in setup.xml
nxt.jar is now self executing and the full classpath is embedded into the jar manifest, see: src/META-INF/MANIFEST.MF which has to be modified whenever changing the classpath, for example when upgrading Jetty
Converting the Jar file to a Windows exe relies on the 7za utility (http://www.7-zip.org/) bundled with the IzPack distribution
Creating the installer setup.jar can be performed either on Unix or Windows
Creating setup.exe must be performed on a Windows workstation