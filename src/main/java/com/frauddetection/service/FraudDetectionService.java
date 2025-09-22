package com.frauddetection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frauddetection.config.KafkaConfig;
import com.frauddetection.model.FraudAlert;
import com.frauddetection.model.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class FraudDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    
    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    // Fraud detection state
    private final Map<String, List<Transaction>> accountTransactionHistory = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastTransactionTime = new ConcurrentHashMap<>();
    private final Set<String> flaggedAccounts = ConcurrentHashMap.newKeySet();
    
    // Fraud detection thresholds
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("100000.00");
    private static final BigDecimal UNUSUAL_VALUE_THRESHOLD = new BigDecimal("50000.00");
    private static final int RAPID_TRADING_THRESHOLD = 5; // transactions in time window
    private static final int RAPID_TRADING_WINDOW_MINUTES = 5;
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    public FraudDetectionService() {
        this.consumer = new KafkaConsumer<>(KafkaConfig.getConsumerProps("fraud-detection-service"));
        this.producer = new KafkaProducer<>(KafkaConfig.getProducerProps());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Subscribe to transactions topic
        this.consumer.subscribe(Collections.singletonList(KafkaConfig.TRANSACTIONS_TOPIC));
        
        // Cleanup old transaction history periodically
        Timer cleanupTimer = new Timer(true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupOldTransactionHistory();
            }
        }, 300000, 300000); // Every 5 minutes
    }

    public void start() {
        logger.info("Starting Fraud Detection Service...");
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        while (running.get()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    processTransaction(record.value());
                }
                
            } catch (Exception e) {
                logger.error("Error processing transactions", e);
                if (!running.get()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.info("Fraud Detection Service stopped.");
    }

    private void processTransaction(String transactionJson) {
        try {
            Transaction transaction = objectMapper.readValue(transactionJson, Transaction.class);
            logger.debug("Processing transaction: {}", transaction.getTransactionId());
            
            // Update transaction history
            updateTransactionHistory(transaction);
            
            // Apply fraud detection rules
            List<FraudAlert> alerts = analyzeTransaction(transaction);
            
            if (!alerts.isEmpty()) {
                // Transaction is suspicious
                for (FraudAlert alert : alerts) {
                    sendFraudAlert(alert);
                }
                logger.warn("Fraud detected for transaction {}: {} alerts generated", 
                          transaction.getTransactionId(), alerts.size());
            } else {
                // Transaction is valid
                sendValidTransaction(transaction);
                logger.debug("Transaction {} validated as legitimate", transaction.getTransactionId());
            }
            
        } catch (Exception e) {
            logger.error("Error processing transaction JSON: {}", transactionJson, e);
        }
    }

    private void updateTransactionHistory(Transaction transaction) {
        String accountId = transaction.getAccountId();
        
        accountTransactionHistory.computeIfAbsent(accountId, k -> new ArrayList<>()).add(transaction);
        lastTransactionTime.put(accountId, transaction.getTimestamp());
        
        // Keep only recent transactions (last hour) to prevent memory leak
        List<Transaction> accountHistory = accountTransactionHistory.get(accountId);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        accountHistory.removeIf(t -> t.getTimestamp().isBefore(oneHourAgo));
    }

    private List<FraudAlert> analyzeTransaction(Transaction transaction) {
        List<FraudAlert> alerts = new ArrayList<>();
        List<String> triggeredRules = new ArrayList<>();
        BigDecimal riskScore = BigDecimal.ZERO;

        // Rule 1: High Value Transaction
        if (transaction.getTotalValue().compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            triggeredRules.add("HIGH_VALUE_TRANSACTION");
            riskScore = riskScore.add(new BigDecimal("0.4"));
        } else if (transaction.getTotalValue().compareTo(UNUSUAL_VALUE_THRESHOLD) >= 0) {
            triggeredRules.add("UNUSUAL_VALUE_TRANSACTION");
            riskScore = riskScore.add(new BigDecimal("0.2"));
        }

        // Rule 2: Rapid Trading Detection
        List<Transaction> recentTransactions = getRecentTransactions(transaction.getAccountId(), 
                                                                    RAPID_TRADING_WINDOW_MINUTES);
        if (recentTransactions.size() >= RAPID_TRADING_THRESHOLD) {
            triggeredRules.add("RAPID_TRADING");
            riskScore = riskScore.add(new BigDecimal("0.3"));
        }

        // Rule 3: Off-Hours Trading
        LocalTime transactionTime = transaction.getTimestamp().toLocalTime();
        if (transactionTime.isBefore(MARKET_OPEN) || transactionTime.isAfter(MARKET_CLOSE)) {
            triggeredRules.add("OFF_HOURS_TRADING");
            riskScore = riskScore.add(new BigDecimal("0.25"));
        }

        // Rule 4: Account Pattern Analysis
        if (analyzeAccountPattern(transaction, recentTransactions)) {
            triggeredRules.add("SUSPICIOUS_ACCOUNT_PATTERN");
            riskScore = riskScore.add(new BigDecimal("0.2"));
        }

        // Rule 5: Previously Flagged Account
        if (flaggedAccounts.contains(transaction.getAccountId())) {
            triggeredRules.add("PREVIOUSLY_FLAGGED_ACCOUNT");
            riskScore = riskScore.add(new BigDecimal("0.15"));
        }

        // Create fraud alert if rules were triggered
        if (!triggeredRules.isEmpty()) {
            FraudAlert.SeverityLevel severity = determineSeverityLevel(riskScore);
            FraudAlert.FraudType fraudType = determineFraudType(triggeredRules);
            
            String description = createAlertDescription(triggeredRules, transaction);
            String alertId = "ALERT-" + UUID.randomUUID().toString().substring(0, 8);
            
            FraudAlert alert = new FraudAlert(
                alertId, 
                transaction.getTransactionId(),
                transaction.getAccountId(),
                fraudType,
                description,
                severity,
                riskScore.min(BigDecimal.ONE), // Cap at 1.0
                transaction,
                triggeredRules
            );
            
            alerts.add(alert);
            
            // Flag account if high risk
            if (riskScore.compareTo(new BigDecimal("0.6")) >= 0) {
                flaggedAccounts.add(transaction.getAccountId());
            }
        }

        return alerts;
    }

    private List<Transaction> getRecentTransactions(String accountId, int windowMinutes) {
        List<Transaction> accountHistory = accountTransactionHistory.get(accountId);
        if (accountHistory == null || accountHistory.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(windowMinutes);
        return accountHistory.stream()
            .filter(t -> t.getTimestamp().isAfter(cutoff))
            .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
            .toList();
    }

    private boolean analyzeAccountPattern(Transaction current, List<Transaction> recent) {
        if (recent.size() < 3) {
            return false;
        }

        // Check for pump and dump pattern (rapid buy then sell of same symbol)
        long buyCount = recent.stream().filter(t -> "BUY".equals(t.getSide())).count();
        long sellCount = recent.stream().filter(t -> "SELL".equals(t.getSide())).count();
        
        // Unusual trading pattern: all transactions same symbol with rapid buy/sell
        String currentSymbol = current.getSymbol();
        boolean allSameSymbol = recent.stream().allMatch(t -> currentSymbol.equals(t.getSymbol()));
        
        return allSameSymbol && buyCount > 0 && sellCount > 0 && (buyCount + sellCount) >= 4;
    }

    private FraudAlert.SeverityLevel determineSeverityLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(new BigDecimal("0.8")) >= 0) {
            return FraudAlert.SeverityLevel.CRITICAL;
        } else if (riskScore.compareTo(new BigDecimal("0.6")) >= 0) {
            return FraudAlert.SeverityLevel.HIGH;
        } else if (riskScore.compareTo(new BigDecimal("0.3")) >= 0) {
            return FraudAlert.SeverityLevel.MEDIUM;
        } else {
            return FraudAlert.SeverityLevel.LOW;
        }
    }

    private FraudAlert.FraudType determineFraudType(List<String> rules) {
        if (rules.contains("HIGH_VALUE_TRANSACTION")) {
            return FraudAlert.FraudType.HIGH_VOLUME;
        } else if (rules.contains("RAPID_TRADING")) {
            return FraudAlert.FraudType.RAPID_TRADING;
        } else if (rules.contains("OFF_HOURS_TRADING")) {
            return FraudAlert.FraudType.OFF_HOURS_TRADING;
        } else if (rules.contains("SUSPICIOUS_ACCOUNT_PATTERN")) {
            return FraudAlert.FraudType.PUMP_AND_DUMP;
        } else {
            return FraudAlert.FraudType.UNUSUAL_PATTERN;
        }
    }

    private String createAlertDescription(List<String> rules, Transaction transaction) {
        StringBuilder desc = new StringBuilder("Suspicious activity detected: ");
        
        if (rules.contains("HIGH_VALUE_TRANSACTION")) {
            desc.append("High-value transaction ($").append(transaction.getTotalValue()).append("). ");
        }
        if (rules.contains("RAPID_TRADING")) {
            desc.append("Rapid trading pattern detected. ");
        }
        if (rules.contains("OFF_HOURS_TRADING")) {
            desc.append("Trading outside market hours. ");
        }
        if (rules.contains("SUSPICIOUS_ACCOUNT_PATTERN")) {
            desc.append("Suspicious account trading pattern. ");
        }
        if (rules.contains("PREVIOUSLY_FLAGGED_ACCOUNT")) {
            desc.append("Previously flagged account activity. ");
        }
        
        return desc.toString().trim();
    }

    private void sendFraudAlert(FraudAlert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.FRAUD_ALERTS_TOPIC,
                alert.getAccountId(),
                json
            );
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send fraud alert: {}", alert.getAlertId(), exception);
                } else {
                    logger.warn("🚨 FRAUD ALERT: {} - {} (Risk: {}) - {}", 
                        alert.getAlertId(), 
                        alert.getFraudType(), 
                        alert.getRiskScore(),
                        alert.getDescription()
                    );
                }
            });
        } catch (Exception e) {
            logger.error("Error sending fraud alert to Kafka", e);
        }
    }

    private void sendValidTransaction(Transaction transaction) {
        try {
            String json = objectMapper.writeValueAsString(transaction);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.VALID_TRANSACTIONS_TOPIC,
                transaction.getAccountId(),
                json
            );
            
            producer.send(record);
        } catch (Exception e) {
            logger.error("Error sending valid transaction to Kafka", e);
        }
    }

    private void cleanupOldTransactionHistory() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        
        accountTransactionHistory.entrySet().removeIf(entry -> {
            List<Transaction> transactions = entry.getValue();
            transactions.removeIf(t -> t.getTimestamp().isBefore(twoHoursAgo));
            return transactions.isEmpty();
        });
        
        lastTransactionTime.entrySet().removeIf(entry -> 
            entry.getValue().isBefore(twoHoursAgo)
        );
        
        logger.debug("Cleaned up old transaction history");
    }

    public void shutdown() {
        logger.info("Shutting down Fraud Detection Service...");
        running.set(false);
        consumer.close();
        producer.close();
    }

    public static void main(String[] args) {
        FraudDetectionService service = new FraudDetectionService();
        service.start();
    }
}