# Run All Components - Real-Time Fraud Detection System
# This script starts all components of the distributed fraud detection system

param(
    [switch]$SkipKafka = $false,
    [switch]$UIOnly = $false
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Starting Real-Time Fraud Detection System" -ForegroundColor Green
Write-Host "=" * 50 -ForegroundColor Green

# Change to project directory
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# Start Kafka infrastructure (unless skipped)
if (-not $SkipKafka) {
    Write-Host "🔧 Starting Kafka infrastructure..." -ForegroundColor Cyan
    & .\scripts\start-kafka.ps1
    
    # Wait a bit for Kafka to fully start
    Write-Host "⏱️ Waiting for Kafka to be fully ready..." -ForegroundColor Yellow
    Start-Sleep -Seconds 15
} else {
    Write-Host "⏭️ Skipping Kafka startup (assumed to be running)" -ForegroundColor Yellow
}

# Build the project first
Write-Host "🔨 Building project..." -ForegroundColor Blue
$buildResult = & .\gradlew build --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed. Please check the errors above." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Build successful" -ForegroundColor Green

if ($UIOnly) {
    Write-Host "🖥️ Starting UI only..." -ForegroundColor Magenta
    & .\gradlew runUI
} else {
    # Start all components in separate terminals
    Write-Host "🔄 Starting all components..." -ForegroundColor Blue
    
    # Start Fraud Detection Service
    Write-Host "Starting Fraud Detection Service..." -ForegroundColor White
    $fraudServiceJob = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; Write-Host '🔍 Fraud Detection Service Starting...' -ForegroundColor Green; .\gradlew runFraudDetectionService" -WindowStyle Normal -PassThru
    
    Start-Sleep -Seconds 3
    
    # Start Transaction Simulator  
    Write-Host "Starting Transaction Simulator..." -ForegroundColor White
    $simulatorJob = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; Write-Host '📊 Transaction Simulator Starting...' -ForegroundColor Blue; .\gradlew runTransactionSimulator" -WindowStyle Normal -PassThru
    
    Start-Sleep -Seconds 3
    
    # Start JavaFX UI
    Write-Host "Starting JavaFX UI..." -ForegroundColor White
    $uiJob = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; Write-Host '🖥️ JavaFX UI Starting...' -ForegroundColor Magenta; .\gradlew runUI" -WindowStyle Normal -PassThru
    
    # Save job information for cleanup
    $jobInfo = @{
        FraudService = $fraudServiceJob.Id
        Simulator = $simulatorJob.Id
        UI = $uiJob.Id
        StartTime = Get-Date
    } | ConvertTo-Json
    
    $jobInfo | Out-File -FilePath ".\scripts\app-processes.json" -Encoding UTF8
    
    Write-Host ""
    Write-Host "🎉 All components are starting up!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 System Components:" -ForegroundColor Cyan
    Write-Host "  - Kafka & ZooKeeper: localhost:9092, localhost:2181" -ForegroundColor White
    Write-Host "  - Fraud Detection Service: Running" -ForegroundColor White  
    Write-Host "  - Transaction Simulator: Generating trades" -ForegroundColor White
    Write-Host "  - JavaFX UI: Real-time dashboard" -ForegroundColor White
    Write-Host ""
    Write-Host "🔧 Management:" -ForegroundColor Yellow
    Write-Host "  - Stop all: .\scripts\stop-all.ps1" -ForegroundColor White
    Write-Host "  - Stop Kafka only: .\scripts\stop-kafka.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "🔍 Monitor the JavaFX UI window for real-time fraud detection!" -ForegroundColor Magenta
    Write-Host "📈 You should see transactions flowing and fraud alerts appearing." -ForegroundColor Blue
    Write-Host ""
    Write-Host "Press any key to continue..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}