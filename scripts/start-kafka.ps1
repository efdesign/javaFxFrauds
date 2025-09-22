# Start Kafka Infrastructure for Fraud Detection System
# This script downloads, configures, and starts Kafka locally

param(
    [switch]$Clean = $false
)

$ErrorActionPreference = "Stop"

# Configuration
$KAFKA_VERSION = "2.13-3.5.1"
$KAFKA_DIR = "kafka_$KAFKA_VERSION"
$KAFKA_URL = "https://downloads.apache.org/kafka/3.5.1/$KAFKA_DIR.tgz"
$INSTALL_DIR = Join-Path $PSScriptRoot "..\kafka-local"

Write-Host "🔧 Kafka Infrastructure Setup for Fraud Detection System" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan

# Clean installation if requested
if ($Clean -and (Test-Path $INSTALL_DIR)) {
    Write-Host "🧹 Cleaning existing Kafka installation..." -ForegroundColor Yellow
    Remove-Item -Path $INSTALL_DIR -Recurse -Force
}

# Create install directory
if (-not (Test-Path $INSTALL_DIR)) {
    New-Item -ItemType Directory -Path $INSTALL_DIR -Force | Out-Null
}

Set-Location $INSTALL_DIR

# Download and extract Kafka if not present
if (-not (Test-Path $KAFKA_DIR)) {
    Write-Host "⬇️ Downloading Kafka $KAFKA_VERSION..." -ForegroundColor Green
    
    # Download using curl (available on Windows 10+)
    $tarFile = "$KAFKA_DIR.tgz"
    if (Get-Command curl -ErrorAction SilentlyContinue) {
        curl -L -o $tarFile $KAFKA_URL
    } else {
        # Fallback to PowerShell's Invoke-WebRequest
        Invoke-WebRequest -Uri $KAFKA_URL -OutFile $tarFile
    }
    
    Write-Host "📦 Extracting Kafka..." -ForegroundColor Green
    
    # Extract tar.gz file (requires tar command on Windows 10+)
    if (Get-Command tar -ErrorAction SilentlyContinue) {
        tar -xzf $tarFile
    } else {
        Write-Error "tar command not found. Please ensure you're running Windows 10+ or install tar utility."
        exit 1
    }
    
    Remove-Item $tarFile
    
    Write-Host "✅ Kafka downloaded and extracted successfully" -ForegroundColor Green
}

# Start ZooKeeper
Write-Host "🚀 Starting ZooKeeper..." -ForegroundColor Blue
$zookeeperCmd = ".\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties"
$zookeeperProcess = Start-Process -FilePath "cmd" -ArgumentList "/c", "cd /d `"$(Join-Path $INSTALL_DIR $KAFKA_DIR)`" && $zookeeperCmd" -WindowStyle Minimized -PassThru

Start-Sleep -Seconds 5

# Start Kafka Server
Write-Host "🚀 Starting Kafka Server..." -ForegroundColor Blue
$kafkaCmd = ".\bin\windows\kafka-server-start.bat .\config\server.properties"
$kafkaProcess = Start-Process -FilePath "cmd" -ArgumentList "/c", "cd /d `"$(Join-Path $INSTALL_DIR $KAFKA_DIR)`" && $kafkaCmd" -WindowStyle Minimized -PassThru

Start-Sleep -Seconds 10

# Create topics
Write-Host "📋 Creating Kafka topics..." -ForegroundColor Magenta
$kafkaPath = Join-Path $INSTALL_DIR $KAFKA_DIR

$topics = @(
    "transactions",
    "fraud-alerts", 
    "valid-transactions"
)

foreach ($topic in $topics) {
    Write-Host "  Creating topic: $topic" -ForegroundColor White
    $createTopicCmd = ".\bin\windows\kafka-topics.bat --create --topic $topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1"
    Start-Process -FilePath "cmd" -ArgumentList "/c", "cd /d `"$kafkaPath`" && $createTopicCmd" -Wait -WindowStyle Hidden
}

# Verify topics
Write-Host "✅ Verifying topics..." -ForegroundColor Green
$listTopicsCmd = ".\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092"
$topicList = & cmd /c "cd /d `"$kafkaPath`" && $listTopicsCmd"

Write-Host "📋 Available topics:" -ForegroundColor Cyan
$topicList | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }

Write-Host ""
Write-Host "🎉 Kafka infrastructure is ready!" -ForegroundColor Green
Write-Host "📊 Kafka is running on: localhost:9092" -ForegroundColor Yellow
Write-Host "🔍 ZooKeeper is running on: localhost:2181" -ForegroundColor Yellow
Write-Host ""
Write-Host "To stop Kafka, close the Kafka and ZooKeeper terminal windows or run:" -ForegroundColor Cyan
Write-Host "  .\scripts\stop-kafka.ps1" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Blue
Write-Host "1. Run the fraud detection service: gradlew runFraudDetectionService" -ForegroundColor White
Write-Host "2. Run the transaction simulator: gradlew runTransactionSimulator" -ForegroundColor White  
Write-Host "3. Run the JavaFX UI: gradlew runUI" -ForegroundColor White
Write-Host ""

# Save process IDs for cleanup
$processInfo = @{
    ZooKeeper = $zookeeperProcess.Id
    Kafka = $kafkaProcess.Id
    StartTime = Get-Date
} | ConvertTo-Json

$processInfo | Out-File -FilePath (Join-Path $PSScriptRoot "kafka-processes.json") -Encoding UTF8