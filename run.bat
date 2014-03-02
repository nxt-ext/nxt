@ECHO OFF 
IF EXIST java (
    start java -cp nxt.jar;lib\*;conf nxt.Nxt
) ELSE (
    IF EXIST "C:\Program Files\Java\jre7" (
    start "C:\Program Files\Java\jre7\bin\java.exe" -cp nxt.jar;lib\*;conf nxt.Nxt
    ) ELSE (
        IF EXIST "C:\Program Files (x86)\Java\jre7" 
        start "C:\Program Files (x86)\Java\jre7\bin\java.exe" -cp nxt.jar;lib\*;conf nxt.Nxt
        ELSE ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
        PAUSE
    )
)