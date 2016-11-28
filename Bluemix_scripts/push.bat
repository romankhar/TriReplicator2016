rem goto DO_THIS
time /T

rem ------------- Define specific war file version
set WAR_FILE=Tri-Replicator.war

rem ------------- Depending on the target space we will define an app URL
set SUFFIX=-%BMX_SPACE%
if "%BMX_SPACE%"=="prod" (
	rem -------- In case the target environment is production we will not use a suffix at the end of the app name or its URL
	set SUFFIX=
)
set BMX_URL=tri-replicator%SUFFIX%
set BMX_APP=TriReplicator
set BMX_DB=ElephantSQL-tri-replicator

rem ------------------ Set project variables
set LIBERTY_SERVER=triServer
set LIBERTY_PKG=%PROJ_PATH%\export\%LIBERTY_SERVER%.zip

rem -------------- Define target space
bluemix target -o %BMX_ORG% -s %BMX_SPACE%

rem "-------------- Package Liberty server along with the app"
call %LIBERTY_HOME%\bin\server.bat stop %LIBERTY_SERVER%
set TMP1=temp
IF NOT EXIST %TMP1% goto skip_dir
rmdir %TMP1% /s /q
:skip_dir
mkdir %TMP1%

rem "------------- Temporarily switch around the Eclipse way of deployment with the full war file - we will later switch back"
move /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\apps\* %TMP1%
copy /Y %PROJ_PATH%\wars\%WAR_FILE% %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\apps
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error

rem "------------- Save current JDBC and logging config files - to be restored later"
copy /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\jdbc.xml %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\jdbc.bak
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error
copy /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\logging.xml %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\logging.bak
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error

rem "------------- Make sure we are deploying into Bluemix with the proper JDBC and logging configuration"
copy /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\jdbc-postgres-bluemix.xml %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\jdbc.xml
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error
copy /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\logging-cloud.xml %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\logging.xml
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error

rem "-------------- Package the server - just the config and war file, not the binaries"
call %LIBERTY_HOME%\bin\server.bat package %LIBERTY_SERVER% --include=usr --archive=%LIBERTY_PKG%
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error

rem "------------- Restore previous JDBC and logging configs we saved earlier"
move /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\jdbc.bak %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\jdbc.xml
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error
move /Y %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\logging.bak %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\logging.xml
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error

rem "--------------- Switch back the files so that local Liberty test server uses the xml and not the war file"
del %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\apps\%WAR_FILE%
copy /Y %TMP1%\* %LIBERTY_HOME%\usr\servers\%LIBERTY_SERVER%\apps

rem "---------------- Push an app as packaged server into Bluemix"
bluemix app push %BMX_APP% -p %LIBERTY_PKG% -n %BMX_URL% -m 384M
@if not $%errorlevel%$==$0$ if not $%errorlevel%$==$8$ goto error

rem "--------- Create database service"
rem "--------- The first attribute is the service name, the second attribute is the plan, and the last attribute is the unique name you are giving to this service instance."
rem "--------- Use 'cf.exe restage' to ensure your env variable changes take effect"
rem bluemix create-service sqldb sqldb_small %BMX_DB%

rem "--------- Bind the database to my app"
bluemix service bind %BMX_APP% %BMX_DB%

rem "--------- Restage the app so that it picks up all settings above"
bluemix app restage %BMX_APP%

@echo "------------------------------------------------------------"
@echo "------    SUCCESS                     ----------------------"
@echo "------------------------------------------------------------"
goto final

:error
@echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" 
@echo "!!!!!!!!!!!        ERROR occured                  !!!!!!!!!!"
@echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" 

:final
time /T