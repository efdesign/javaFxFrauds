# Real-Time Fraud Detection with JavaFX & Kafka

A distributed fraud detection system that simulates Alpaca stock transactions and detects fraudulent patterns in real-time using Kafka and JavaFX.

## 🎬 Demo Video

https://github.com/efdesign/javaFxFrauds/blob/master/docs/fxRealtime.mp4

_Watch the system in action: real-time transaction processing, fraud detection, and live UI updates_

## Architecture

```
Alpaca Simulator → Kafka (transactions) → Fraud Detection Service → Kafka (fraud-alerts/valid-transactions) → JavaFX UI
```

## Components

1. **Transaction Simulator**: Generates fake stock transactions
2. **Fraud Detection Service**: Analyzes transactions for suspicious patterns
3. **JavaFX UI**: Real-time display of transactions and alerts
4. **Kafka**: Message broker for distributed communication

## 🚀 Quick Start

**One-command system startup:**

```powershell
.\scripts\start.ps1
```

**To stop the system:**

```powershell
.\scripts\stop.ps1
```

The start script will:

- Build the project with a fresh Gradle daemon
- Start Kafka 4.1.0 in KRaft mode (no ZooKeeper needed)
- Create required Kafka topics
- Launch all services in separate PowerShell windows
- Display system status and log locations

## ⚙️ Manual Development Commands (Optional)

If you need to run individual components for development:

```powershell
# Build project
.\gradlew build

# Run individual services (each in separate terminal)
.\gradlew runFraudDetectionService
.\gradlew runTransactionSimulator
.\gradlew runUI
```

**Note**: The `start.ps1` script is the recommended way to run the complete system.

## System Requirements

- **Windows 10/11** with PowerShell
- **Java 17+**
- **Apache Kafka 4.1.0** (included)
- **Gradle** (wrapper included)

## Fraud Detection Rules

- High volume trades (>$100,000)
- Rapid successive trades from same account
- Unusual trading hours (outside 9:30 AM - 4:00 PM EST)
- Price manipulation patterns

## 🔧 Troubleshooting

**System Logs:**

- `logs\fraud-detection.log` - Application logs
- `logs\kafka.log` - Kafka server logs
- `logs\kafka.err` - Kafka error logs

**Common Issues:**

- **Build failures**: Run `.\gradlew clean build`
- **Kafka connection issues**: Check `logs\kafka.log` for errors
- **Port conflicts**: Kafka uses port 9092, make sure it's available
- **JavaFX issues**: Ensure Java 17+ is installed

**System Status:**
The start script shows component status and process IDs. Each service runs in its own PowerShell window for easy monitoring.

## 📁 Documentation

For detailed system documentation, see the [`docs/`](./docs/) folder:

- [`docs/1.0.0-release-notes.md`](./docs/1.0.0-release-notes.md) - Complete system documentation and setup guide
- [`docs/1.0.0-project-status.md`](./docs/1.0.0-project-status.md) - Current project status and achievements
- [`docs/1.0.1-memory-analysis.md`](./docs/1.0.1-memory-analysis.md) - Memory usage analysis and optimization guide
