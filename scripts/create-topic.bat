@echo off
cd kafka_2.13-2.8.0\bin\windows
kafka-topics.bat --create --topic %1 --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1