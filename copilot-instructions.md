# Copilot Instructions for Real-Time Fraud Detection System

This document provides standardized instructions for AI systems (GitHub Copilot, Claude Sonnet, Grok, ChatGPT-4, etc.) working with this JavaFX + Kafka fraud detection project.

## 🖥️ Platform & Environment Requirements

### Windows Environment

- **Operating System**: Windows 10/11
- **Shell**: PowerShell (primary)
- **Command Concatenation**: Use semicolon (`;`) to chain PowerShell commands
  ```powershell
  cd D:\ProgettiD\javaFxRealtimeFraudulentTransactions; .\gradlew build; .\scripts\run-all.ps1
  ```

### Critical Platform Rules

1. **ALWAYS** use PowerShell syntax for Windows commands
2. **NEVER** use Linux/bash syntax (&&, ||) for command chaining
3. **ALWAYS** use backslashes (`\`) for Windows file paths
4. **MUST** use semicolon (`;`) for PowerShell command concatenation

## 📋 Documentation Standards

### Documentation File Organization

**LOCATION**: All documentation files **MUST** be placed in the `docs/` folder

**STRUCTURE**:

```
project-root/
├── README.md           # Main project README (stays in root)
├── docs/              # All other documentation goes here
│   ├── 1.0.0-release-notes.md
│   ├── 1.0.0-project-status.md
│   ├── 1.0.1-memory-analysis.md
│   └── 1.2.0-architecture-updates.md
└── [other project files]
```

### Markdown File Naming Convention

All documentation files **MUST** follow this exact pattern:

```
docs/{major}.{minor}.{patch}-{description}.md
```

**Key Rules**:

- **Version first**: Start with version number (e.g., `1.0.1-`)
- **Lowercase description**: Use lowercase with hyphens (e.g., `-memory-analysis`)
- **Descriptive suffix**: Clear, concise description of content
- **Location**: Always in `docs/` folder (except README.md)

**Examples:**

- `docs/1.0.0-release-notes.md` - Release documentation
- `docs/1.0.0-project-status.md` - Current project state
- `docs/1.0.1-memory-analysis.md` - Memory usage analysis
- `docs/1.1.0-architecture-updates.md` - Architecture changes
- `docs/2.0.0-deployment-guide.md` - Deployment procedures
- `docs/1.0.2-performance-benchmarks.md` - Performance analysis

### Documentation Rules

1. **Location**: All docs in `docs/` folder (README.md stays in root)
2. **Version Format**: `{version}-{description}.md` (version first, lowercase description)
3. **Master Documents**: Mark critical docs as IMMUTABLE in header
4. **Amendments**: Create new versioned files for major changes
5. **Sequential Updates**: Always increment version numbers for updates
6. **Descriptive Names**: Use clear, descriptive names with hyphens

## 🔄 Git Workflow & Version Control

### Standard Git Process

1. **Code Development**: Implement feature/fix
2. **Documentation**: Create/update versioned `.md` file with changes
3. **Testing**: Run all tests (unit + e2e) - ALL MUST PASS
4. **Commit Code**: Commit implementation with descriptive message
5. **Commit Documentation**: Separate commit for documentation iteration
6. **Tag**: Tag commits with semantic version when applicable

### Commit Message Format

```
feat: implement real-time transaction processing

- Add Kafka consumer for transaction stream
- Implement fraud detection rules engine
- Add JavaFX UI for real-time monitoring

Tests: ✅ All unit and e2e tests passing
Docs: docs/2.1.0-implementation-log.md
```

### Documentation Commit Example

```
docs: add implementation log v2.1.0

- Document transaction processing implementation
- Update architecture decisions
- Record performance benchmarks

File: docs/2.1.0-implementation-log.md
```

## 🚀 Server & Script Requirements

### Non-Blocking Server Execution

**CRITICAL**: All server startup scripts MUST launch in separate, non-blocking shells.

#### ✅ Correct Server Startup

```powershell
# In run-all.ps1 - ALWAYS use separate processes
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; .\gradlew runTransactionSimulator"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; .\gradlew runFraudDetectionService"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; .\gradlew runUI"
```

#### ❌ Incorrect Server Startup

```powershell
# NEVER do this - blocks execution
.\gradlew runTransactionSimulator
.\gradlew runFraudDetectionService
.\gradlew runUI
```

### Script Standards

1. **Always** start servers in separate PowerShell windows
2. **Always** use `-NoExit` to keep server windows open
3. **Always** change to project directory in each new process
4. **Always** provide process cleanup in stop scripts

## ☕ Java Development Standards

### Framework Preferences

1. **Primary**: Spring Boot for enterprise features
2. **Dependency Injection**: Use Spring's `@Autowired`, `@Component`, `@Service`
3. **Configuration**: Use `@ConfigurationProperties` and `application.properties`
4. **Logging**: SLF4J with Logback configuration
5. **Testing**: JUnit 5 + Spring Boot Test + TestContainers

### Code Structure

```
src/
├── main/java/com/frauddetection/
│   ├── config/          # Spring configurations
│   ├── controller/      # REST controllers (if applicable)
│   ├── service/         # Business logic services
│   ├── repository/      # Data access layer
│   ├── model/          # Data models/entities
│   ├── dto/            # Data transfer objects
│   └── ui/             # JavaFX UI components
├── main/resources/
│   ├── application.properties
│   ├── logback.xml
│   └── static/         # Static resources
└── test/java/com/frauddetection/
    ├── integration/    # Integration tests
    ├── unit/          # Unit tests
    └── e2e/           # End-to-end tests
```

### Spring Boot Integration

- **Replace** standalone Kafka configs with Spring Boot starters
- **Use** `@KafkaListener` for consumers
- **Use** `@Service` for business logic
- **Use** `@Component` for UI components
- **Use** Spring profiles for environment-specific configs

## 🧪 Testing Requirements

### Test Coverage Requirements

- **Unit Tests**: 90%+ coverage for service layers
- **Integration Tests**: All Kafka producers/consumers
- **E2E Tests**: Complete workflow from simulation to UI
- **ALL TESTS MUST PASS** before any commit

### Test Structure

```java
@SpringBootTest
@TestPropertySource(properties = {
    "kafka.bootstrap-servers=localhost:9092",
    "logging.level.com.frauddetection=DEBUG"
})
class FraudDetectionServiceTest {

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Test
    void shouldDetectHighValueFraud() {
        // Given, When, Then
    }
}
```

### E2E Test Requirements

1. **Start embedded Kafka** for tests
2. **Test complete flow**: Transaction → Detection → Alert → UI
3. **Verify UI updates** with TestFX for JavaFX testing
4. **Clean up resources** after each test

## 🔧 Build & Deployment Standards

### Gradle Configuration

- **Always** use Gradle wrapper (`./gradlew`)
- **Include** Spring Boot Gradle plugin
- **Configure** JavaFX Gradle plugin properly
- **Set** Java 17+ as minimum version

### Task Definitions

```gradle
// Required Gradle tasks
task runTransactionSimulator(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.frauddetection.FraudDetectionApplication'
    args = ['--spring.profiles.active=simulator']
}

task runFraudDetectionService(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.frauddetection.FraudDetectionApplication'
    args = ['--spring.profiles.active=fraud-service']
}

task runUI(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.frauddetection.FraudDetectionApplication'
    args = ['--spring.profiles.active=ui']
}
```

## 🚨 Critical Rules Summary

### MUST ALWAYS Follow

1. ✅ **Use PowerShell syntax** on Windows
2. ✅ **Start servers in non-blocking shells**
3. ✅ **Use semantic versioning** for all `.md` files
4. ✅ **Run all tests before committing** (unit + e2e)
5. ✅ **Use Spring Boot** where possible
6. ✅ **Separate commits** for code and documentation
7. ✅ **Never modify master plan** (1.0.0.plan.md)

### NEVER Do

1. ❌ Use Linux/bash syntax on Windows
2. ❌ Block server execution in scripts
3. ❌ Commit without running tests
4. ❌ Modify existing versioned documentation
5. ❌ Use hardcoded configurations
6. ❌ Skip integration tests

## 🎯 Success Criteria

Before marking any task complete:

- [ ] All unit tests pass (`.\gradlew test`)
- [ ] All integration tests pass (`.\gradlew integrationTest`)
- [ ] E2E tests validate complete workflow
- [ ] Documentation updated with proper versioning
- [ ] Code follows Spring Boot best practices
- [ ] Servers start in non-blocking mode
- [ ] Git commits include both code and docs

---

**⚠️ These instructions are MANDATORY for all AI systems working on this project. Deviation from these guidelines is not acceptable.**
