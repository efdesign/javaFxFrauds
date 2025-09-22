# Create Kafka Topic PowerShell Script
param([string]$TopicName)

Set-Location "kafka"
& ".\bin\windows\kafka-topics.bat" --create --topic $TopicName --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1