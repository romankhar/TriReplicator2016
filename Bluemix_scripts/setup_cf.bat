echo off

set PROJ_PATH=c:\projects_c\Tri-Replicator-16
set LIBERTY_HOME=%PROJ_PATH%\wlp
set JAVA_HOME=%LIBERTY_HOME%\java\java
set PATH=%PATH%;%JAVA_HOME%\bin;%LIBERTY_HOME%\bin

set BMX_ORG=Tri-Replicator-Org
set BMX_USER=romanik
set BMX_PASSWORD=put-real-password-here

rem ------------- Define the deployment target space
rem set BMX_SPACE=dev
rem set BMX_SPACE=test
set BMX_SPACE=prod

bluemix config --color true
bluemix api api.ng.bluemix.net
bluemix login -u %BMX_USER% -p %BMX_PASSWORD% -o %BMX_ORG% -s %BMX_SPACE%