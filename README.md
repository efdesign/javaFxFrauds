# Real-Time Fraud Detection with JavaFX & Kafka (interview demo app)

A distributed fraud detection system that simulates Alpaca stock transactions and detects fraudulent patterns in real-time using Kafka and JavaFX.

## 🎬 Demo Video

https://github.com/user-attachments/assets/8f7927f7-3de6-4451-bd02-73b3761c3e1c


*Watch the system in action: real-time transaction processing, fraud detection, and live UI updates*

## Architecture

```
Alpaca Simulator → Kafka (transactions) → Fraud Detection Service → Kafka (fraud-alerts/valid-transactions) → JavaFX UI
```

## Components

1. **Transaction Simulator**: Generates fake stock transactions
2. **Fraud Detection Service**: Analyzes transactions for suspicious patterns
3. **JavaFX UI**: Real-time display of transactions and alerts
4. **Kafka**: Message broker for distributed communication

## Quick Start

1. Start Kafka: `.\scripts\start-kafka.ps1`
2. Run components: `.\scripts\run-all.ps1`

## Manual Start

```bash
# Terminal 1: Start Kafka
.\scripts\start-kafka.ps1

# Terminal 2: Start Fraud Detection Service
.\gradlew runFraudDetectionService

# Terminal 3: Start Transaction Simulator
.\gradlew runTransactionSimulator

# Terminal 4: Start JavaFX UI
.\gradlew runUI
```

## Fraud Detection Rules

- High volume trades (>$100,000)
- Rapid successive trades from same account
- Unusual trading hours (outside 9:30 AM - 4:00 PM EST)
- Price manipulation patterns

## 📁 Documentation

For detailed system documentation, see the [`docs/`](./docs/) folder:

- [`docs/1.0.0-release-notes.md`](./docs/1.0.0-release-notes.md) - Complete system documentation and setup guide
- [`docs/1.0.0-project-status.md`](./docs/1.0.0-project-status.md) - Current project status and achievements
- [`docs/1.0.1-memory-analysis.md`](./docs/1.0.1-memory-analysis.md) - Memory usage analysis and optimization guide
