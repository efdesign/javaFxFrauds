param(
    [string]$Action = "random-uuid",
    [string]$ConfigFile = "",
    [string]$ClusterUUID = ""
)

# Set working directory
Set-Location "d:\ProgettiD\javaFxRealtimeFraudulentTransactions"

# Set shorter environment variables
$env:KAFKA_HOME = "d:\ProgettiD\javaFxRealtimeFraudulentTransactions\kafka"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

Write-Host "Kafka Storage Tool - Action: $Action"

if ($Action -eq "random-uuid") {
    Write-Host "Generating random UUID..."
    & "$env:KAFKA_HOME\bin\windows\kafka-storage.bat" random-uuid
}
elseif ($Action -eq "format" -and $ConfigFile -ne "" -and $ClusterUUID -ne "") {
    Write-Host "Formatting storage with UUID: $ClusterUUID"
    & "$env:KAFKA_HOME\bin\windows\kafka-storage.bat" format -t $ClusterUUID -c $ConfigFile
}
else {
    Write-Host "Usage: .\kafka-storage-wrapper.ps1 [-Action random-uuid|format] [-ConfigFile path] [-ClusterUUID uuid]"
    Write-Host "Examples:"
    Write-Host "  .\kafka-storage-wrapper.ps1 -Action random-uuid"
    Write-Host "  .\kafka-storage-wrapper.ps1 -Action format -ConfigFile 'kafka\config\server.properties' -ClusterUUID 'your-uuid'"
}