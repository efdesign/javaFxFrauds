# Stop Kafka Infrastructure
# This script stops ZooKeeper and Kafka servers

$ErrorActionPreference = "Stop"

Write-Host "🛑 Stopping Kafka Infrastructure..." -ForegroundColor Red
Write-Host "=" * 40 -ForegroundColor Red

$processFile = Join-Path $PSScriptRoot "kafka-processes.json"

if (Test-Path $processFile) {
    try {
        $processInfo = Get-Content $processFile | ConvertFrom-Json
        
        Write-Host "🔍 Found running Kafka processes..." -ForegroundColor Yellow
        
        # Stop Kafka Server
        if ($processInfo.Kafka) {
            Write-Host "Stopping Kafka Server (PID: $($processInfo.Kafka))..." -ForegroundColor White
            try {
                Stop-Process -Id $processInfo.Kafka -Force -ErrorAction SilentlyContinue
                Write-Host "✅ Kafka Server stopped" -ForegroundColor Green
            } catch {
                Write-Host "⚠️ Could not stop Kafka Server (may already be stopped)" -ForegroundColor Yellow
            }
        }
        
        # Stop ZooKeeper
        if ($processInfo.ZooKeeper) {
            Write-Host "Stopping ZooKeeper (PID: $($processInfo.ZooKeeper))..." -ForegroundColor White
            try {
                Stop-Process -Id $processInfo.ZooKeeper -Force -ErrorAction SilentlyContinue
                Write-Host "✅ ZooKeeper stopped" -ForegroundColor Green
            } catch {
                Write-Host "⚠️ Could not stop ZooKeeper (may already be stopped)" -ForegroundColor Yellow
            }
        }
        
        # Remove process file
        Remove-Item $processFile -ErrorAction SilentlyContinue
        
    } catch {
        Write-Host "❌ Error reading process file: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "🔍 No process file found. Attempting to find and stop Kafka processes..." -ForegroundColor Yellow
    
    # Try to find and stop Kafka/ZooKeeper processes
    $kafkaProcesses = Get-Process | Where-Object { $_.ProcessName -like "*kafka*" -or $_.ProcessName -like "*zookeeper*" -or $_.MainWindowTitle -like "*kafka*" -or $_.MainWindowTitle -like "*zookeeper*" }
    
    if ($kafkaProcesses) {
        Write-Host "Found $($kafkaProcesses.Count) Kafka-related processes" -ForegroundColor White
        foreach ($process in $kafkaProcesses) {
            Write-Host "Stopping process: $($process.ProcessName) (PID: $($process.Id))" -ForegroundColor White
            try {
                Stop-Process -Id $process.Id -Force
                Write-Host "✅ Stopped $($process.ProcessName)" -ForegroundColor Green
            } catch {
                Write-Host "⚠️ Could not stop $($process.ProcessName)" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "ℹ️ No Kafka processes found running" -ForegroundColor Blue
    }
}

# Also try to stop any Java processes that might be Kafka
Write-Host "🔍 Checking for Java processes (Kafka runs on Java)..." -ForegroundColor Yellow
$javaProcesses = Get-Process java -ErrorAction SilentlyContinue | Where-Object { 
    $_.CommandLine -like "*kafka*" -or $_.CommandLine -like "*zookeeper*" 
}

if ($javaProcesses) {
    Write-Host "Found $($javaProcesses.Count) Java/Kafka processes" -ForegroundColor White
    foreach ($process in $javaProcesses) {
        Write-Host "Stopping Java process (PID: $($process.Id))" -ForegroundColor White
        try {
            Stop-Process -Id $process.Id -Force
            Write-Host "✅ Stopped Java process" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ Could not stop Java process" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "🎯 Kafka infrastructure stop completed!" -ForegroundColor Green
Write-Host "📊 All Kafka and ZooKeeper processes should be stopped." -ForegroundColor Cyan
Write-Host ""