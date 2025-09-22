# Stop All Components - Real-Time Fraud Detection System
# This script stops all running components

$ErrorActionPreference = "Stop"

Write-Host "🛑 Stopping Real-Time Fraud Detection System" -ForegroundColor Red
Write-Host "=" * 45 -ForegroundColor Red

# Change to project directory
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# Stop application processes
$processFile = ".\scripts\app-processes.json"

if (Test-Path $processFile) {
    try {
        Write-Host "🔍 Stopping application processes..." -ForegroundColor Yellow
        $processInfo = Get-Content $processFile | ConvertFrom-Json
        
        $processes = @(
            @{ Name = "JavaFX UI"; Id = $processInfo.UI },
            @{ Name = "Transaction Simulator"; Id = $processInfo.Simulator },
            @{ Name = "Fraud Detection Service"; Id = $processInfo.FraudService }
        )
        
        foreach ($proc in $processes) {
            if ($proc.Id) {
                Write-Host "Stopping $($proc.Name) (PID: $($proc.Id))..." -ForegroundColor White
                try {
                    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                    Write-Host "✅ $($proc.Name) stopped" -ForegroundColor Green
                }
                catch {
                    Write-Host "⚠️ Could not stop $($proc.Name) (may already be stopped)" -ForegroundColor Yellow
                }
            }
        }
        
        # Remove process file
        Remove-Item $processFile -ErrorAction SilentlyContinue
        
    }
    catch {
        Write-Host "❌ Error reading process file: $($_.Exception.Message)" -ForegroundColor Red
    }
}
else {
    Write-Host "🔍 No app process file found. Attempting to find and stop related processes..." -ForegroundColor Yellow
    
    # Try to find and stop Java processes related to our app
    $javaProcesses = Get-Process java -ErrorAction SilentlyContinue | Where-Object { 
        $_.CommandLine -like "*frauddetection*" -or 
        $_.CommandLine -like "*FraudDetectionApp*" -or
        $_.CommandLine -like "*AlpacaTransactionSimulator*" -or
        $_.CommandLine -like "*FraudDetectionService*"
    }
    
    if ($javaProcesses) {
        Write-Host "Found $($javaProcesses.Count) application processes" -ForegroundColor White
        foreach ($process in $javaProcesses) {
            Write-Host "Stopping application process (PID: $($process.Id))" -ForegroundColor White
            try {
                Stop-Process -Id $process.Id -Force
                Write-Host "✅ Stopped application process" -ForegroundColor Green
            }
            catch {
                Write-Host "⚠️ Could not stop application process" -ForegroundColor Yellow
            }
        }
    }
}

# Stop Kafka infrastructure
Write-Host "🔧 Stopping Kafka infrastructure..." -ForegroundColor Cyan
& .\scripts\stop-kafka.ps1

Write-Host ""
Write-Host "🎯 Fraud Detection System shutdown completed!" -ForegroundColor Green
Write-Host "📊 All components should be stopped." -ForegroundColor Cyan
Write-Host ""