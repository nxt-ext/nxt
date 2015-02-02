@ECHO OFF
start "NXT NRS" jre\bin\java.exe -server -cp classes;lib\*;conf -Dnxt.runtime.mode=desktop nxt.Nxt