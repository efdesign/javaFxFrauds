@echo off
cd /d "d:\ProgettiD\javaFxRealtimeFraudulentTransactions"
echo Generating Kafka cluster UUID...
call "kafka\bin\windows\kafka-storage.bat" random-uuid > temp-uuid.txt
set /p CLUSTER_UUID=<temp-uuid.txt
echo Cluster UUID: %CLUSTER_UUID%
echo.
echo Initializing Kafka cluster metadata...
call "kafka\bin\windows\kafka-storage.bat" format -t %CLUSTER_UUID% -c "kafka\config\server.properties"
del temp-uuid.txt
echo.
echo Kafka cluster initialization complete!
pause