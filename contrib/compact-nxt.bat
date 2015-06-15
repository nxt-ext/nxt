@REM Compact the Nxt NRS database
@echo *********************************************************************
@echo * This batch file will compact and reorganize the Nxt NRS database. *
@echo * This process can take a long time.  Do not interrupt the batch    *
@echo * file or shutdown the computer until it finishes.                  *
@echo *********************************************************************
@setlocal

@REM NXTDIR is the Nxt application directory
@set NXTDIR=%APPDATA%\Nxt

@REM NRSDIR is the Nxt install directory
@set NRSDIR=C:\Program Files\Nxt

@if exist "%NXTDIR%\nxt_db\nxt.h2.db" goto COMPACT
@if exist "%NXTDIR%\nxt_db\nxt.mv.db" goto COMPACT
@echo The Nxt NRS database does not exist
@goto DONE

:COMPACT
@if exist "%NXTDIR%\nxt_db\backup.sql.gz" (del %NXTDIR%\nxt_db\backup.sql.gz)
@echo Creating the database SQL script
java -Xmx768m -cp "%NRSDIR%\lib\*" org.h2.tools.Script -script "%NXTDIR%\nxt_db\backup.sql.gz" -url "jdbc:h2:%NXTDIR%\nxt_db\nxt" -user sa -password "sa" -options COMPRESSION GZIP
@if exist "%NXTDIR%\nxt_db\backup.sql.gz" goto RECREATE
@echo "Unable to create the database SQL script"
goto DONE

:RECREATE
@echo Recreating the Nxt NRS database
@if exist "%NXTDIR%\nxt_db\nxt.mv.db" (del %NXTDIR%\nxt_db\nxt.mv.db)
@if exist "%NXTDIR%\nxt_db\nxt.h2.db" (del %NXTDIR%\nxt_db\nxt.h2.db)
java -Xmx768m -cp "%NRSDIR%\lib\*" org.h2.tools.RunScript -script "%NXTDIR%\nxt_db\backup.sql.gz" -url "jdbc:h2:%NXTDIR%\nxt_db\nxt" -user sa -password "sa" -options COMPRESSION GZIP
@if exist "%NXTDIR%\nxt_db\nxt.mv.db" goto SUCCESS
@if exist "%NXTDIR%\nxt_db\nxt.h2.db" goto SUCCESS
@echo Unable to create the Nxt NRS database, backup script is %NXTDIR%\nxt_db\backup.sql.gz
goto DONE

:SUCCESS
@echo Nxt NRS database compacted, backup script is %NXTDIR%\nxt_db\backup.sql.gz

:DONE
@endlocal
