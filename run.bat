@ECHO OFF
start "NXT NRS" jre\bin\java.exe -cp classes;lib\*;conf -Dnxt.properties=${USER_HOME}\AppData\Roaming\NXT\nxt.properties nxt.Nxt