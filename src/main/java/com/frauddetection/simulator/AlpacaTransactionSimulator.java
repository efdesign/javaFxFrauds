package com.frauddetection.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frauddetection.config.KafkaConfig;
import com.frauddetection.model.Transaction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlpacaTransactionSimulator {

    private static final Logger logger = LoggerFactory.getLogger(AlpacaTransactionSimulator.class);

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final Random random;
    private final ScheduledExecutorService executor;

    // Simulated stock symbols
    private final List<String> symbols = Arrays.asList(
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "NVDA", "META", "NFLX", "BABA", "AMD",
            "INTC", "CRM", "ORCL", "ADBE", "PYPL", "UBER", "LYFT", "SPOT", "ZOOM", "SQ");

    // Simulated account IDs
    private final List<String> accountIds = Arrays.asList(
            "ACC001", "ACC002", "ACC003", "ACC004", "ACC005", "ACC006", "ACC007", "ACC008", "ACC009", "ACC010");

    public AlpacaTransactionSimulator() {
        this.producer = new KafkaProducer<>(KafkaConfig.getProducerProps());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.random = new Random();
        this.executor = Executors.newScheduledThreadPool(2);
    }

    public void startSimulation() {
        logger.info("Starting Alpaca transaction simulation...");

        // Normal transactions - every 5 seconds for easier debugging
        logger.info("Generating normal transactions every 5 seconds");
        executor.scheduleWithFixedDelay(this::generateNormalTransaction, 0, 5, TimeUnit.SECONDS);

        // Suspicious transactions - less frequently for debugging
        logger.info("Generating suspicious transactions every 60 seconds");
        executor.scheduleWithFixedDelay(this::generateSuspiciousTransaction, 10, 60, TimeUnit.SECONDS);

        // Burst of transactions - even less frequent for debugging
        logger.info("Generating transaction bursts every 180 seconds");
        executor.scheduleWithFixedDelay(this::generateBurstTransactions, 60, 180, TimeUnit.SECONDS);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        logger.info("Transaction simulation started. Press Ctrl+C to stop.");
    }

    private void generateNormalTransaction() {
        try {
            Transaction transaction = createRandomTransaction(false);
            sendTransaction(transaction);

            // Add some randomness to timing
            if (random.nextBoolean()) {
                Thread.sleep(random.nextInt(3000) + 500);
            }
        } catch (Exception e) {
            logger.error("Error generating normal transaction", e);
        }
    }

    private void generateSuspiciousTransaction() {
        try {
            Transaction transaction = createRandomTransaction(true);
            sendTransaction(transaction);
            logger.warn("Generated suspicious transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Error generating suspicious transaction", e);
        }
    }

    private void generateBurstTransactions() {
        try {
            int burstCount = random.nextInt(5) + 3; // 3-7 transactions
            String accountId = accountIds.get(random.nextInt(accountIds.size()));
            String symbol = symbols.get(random.nextInt(symbols.size()));

            logger.info("Generating burst of {} transactions for account {} trading {}",
                    burstCount, accountId, symbol);

            for (int i = 0; i < burstCount; i++) {
                Transaction transaction = createBurstTransaction(accountId, symbol);
                sendTransaction(transaction);
                Thread.sleep(100 + random.nextInt(200)); // Very rapid succession
            }
        } catch (Exception e) {
            logger.error("Error generating burst transactions", e);
        }
    }

    private Transaction createRandomTransaction(boolean makeSuspicious) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        String accountId = accountIds.get(random.nextInt(accountIds.size()));
        String symbol = symbols.get(random.nextInt(symbols.size()));
        String side = random.nextBoolean() ? "BUY" : "SELL";

        BigDecimal basePrice = getBasePrice(symbol);
        BigDecimal price = basePrice.add(new BigDecimal(random.nextGaussian() * 5.0))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal quantity;
        if (makeSuspicious) {
            // Make suspicious: either very high volume or very high value
            if (random.nextBoolean()) {
                quantity = new BigDecimal(random.nextInt(10000) + 5000); // High volume
            } else {
                quantity = new BigDecimal(random.nextInt(100) + 1);
                price = new BigDecimal(random.nextInt(5000) + 1000); // High price to make total value suspicious
            }
        } else {
            quantity = new BigDecimal(random.nextInt(1000) + 1);
        }

        LocalDateTime timestamp = LocalDateTime.now();

        // Make some transactions outside normal trading hours if suspicious
        if (makeSuspicious && random.nextBoolean()) {
            int hour = random.nextBoolean() ? random.nextInt(9) + 1 : random.nextInt(7) + 17; // 1-9 AM or 5-11 PM
            timestamp = timestamp.withHour(hour).withMinute(random.nextInt(60));
        }

        return new Transaction(transactionId, accountId, symbol, side, quantity, price, timestamp);
    }

    private Transaction createBurstTransaction(String accountId, String symbol) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        String side = random.nextBoolean() ? "BUY" : "SELL";
        BigDecimal basePrice = getBasePrice(symbol);
        BigDecimal price = basePrice.add(new BigDecimal(random.nextGaussian() * 2.0))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal quantity = new BigDecimal(random.nextInt(500) + 10);
        LocalDateTime timestamp = LocalDateTime.now();

        return new Transaction(transactionId, accountId, symbol, side, quantity, price, timestamp);
    }

    private BigDecimal getBasePrice(String symbol) {
        // Simulated base prices for different stocks
        return switch (symbol) {
            case "AAPL" -> new BigDecimal("175.00");
            case "GOOGL" -> new BigDecimal("135.00");
            case "MSFT" -> new BigDecimal("330.00");
            case "AMZN" -> new BigDecimal("140.00");
            case "TSLA" -> new BigDecimal("250.00");
            case "NVDA" -> new BigDecimal("450.00");
            case "META" -> new BigDecimal("320.00");
            case "NFLX" -> new BigDecimal("380.00");
            default -> new BigDecimal("100.00");
        };
    }

    private void sendTransaction(Transaction transaction) {
        try {
            String json = objectMapper.writeValueAsString(transaction);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.TRANSACTIONS_TOPIC,
                    transaction.getAccountId(),
                    json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send transaction: {}", transaction.getTransactionId(), exception);
                } else {
                    logger.info("Sent transaction: {} - {} {} {}@${} (total: ${})",
                            transaction.getTransactionId(),
                            transaction.getSide(),
                            transaction.getQuantity(),
                            transaction.getSymbol(),
                            transaction.getPrice(),
                            transaction.getTotalValue());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending transaction to Kafka", e);
        }
    }

    public void shutdown() {
        logger.info("Shutting down transaction simulator...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        producer.close();
        logger.info("Transaction simulator stopped.");
    }

    public static void main(String[] args) {
        AlpacaTransactionSimulator simulator = new AlpacaTransactionSimulator();
        simulator.startSimulation();

        // Keep the main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("Simulation interrupted");
            simulator.shutdown();
        }
    }
}