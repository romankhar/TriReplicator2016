rem -------------- list all of the files
rem cf files %BMX_APP%
rem cf files %BMX_APP% app/wlp/usr/servers/%LIBERTY_SERVER%/lib

rem ------------- this will get files from BlueMix into local file system - for inspection and debugging
cf files %BMX_APP% app/wlp/usr/servers/%LIBERTY_SERVER%/server.xml > server.xml
rem cf files %BMX_APP% app/wlp/usr/servers/%LIBERTY_SERVER%/runtime-vars.xml 

rem -------------- this will get events from the app
rem cf events %BMX_APP%

rem ------------- list all possible BlueMix services
rem bluemix marketplace

rem ------------- this will tail logs from the app
rem cf logs %BMX_APP%