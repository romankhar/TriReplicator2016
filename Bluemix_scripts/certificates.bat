call setenv.bat

rem ------------------- Generate certificate request
keytool -certreq -alias iisSSL -storetype PKCS12 -storepass nikogda-Ne-Budet-sobaka-3-kot -keystore %LIBERTY_HOME%\iis-server-keystore.p12 -file certreq.req -v