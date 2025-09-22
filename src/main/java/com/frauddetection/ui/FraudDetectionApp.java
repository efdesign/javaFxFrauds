package com.frauddetection.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.frauddetection.config.KafkaConfig;
import com.frauddetection.model.FraudAlert;
import com.frauddetection.model.Transaction;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FraudDetectionApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionApp.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // UI Components
    private TableView<TransactionDisplayModel> transactionTable;
    private TableView<FraudAlertDisplayModel> alertTable;
    private Label transactionCountLabel;
    private Label alertCountLabel;
    private Label statusLabel;
    private ProgressIndicator statusIndicator;

    // Data
    private final ObservableList<TransactionDisplayModel> transactions = FXCollections.observableArrayList();
    private final ObservableList<FraudAlertDisplayModel> alerts = FXCollections.observableArrayList();

    // Kafka consumers
    private KafkaConsumer<String, String> transactionConsumer;
    private KafkaConsumer<String, String> alertConsumer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService kafkaExecutor;

    // Counters
    private final AtomicInteger transactionCount = new AtomicInteger(0);
    private final AtomicInteger alertCount = new AtomicInteger(0);

    @Override
    public void start(Stage primaryStage) {
        objectMapper.registerModule(new JavaTimeModule());

        primaryStage.setTitle("Real-Time Fraud Detection System");
        primaryStage.setOnCloseRequest(e -> cleanup());

        // Create main layout
        BorderPane mainLayout = createMainLayout();

        // Setup Kafka consumers
        setupKafkaConsumers();

        // Create scene
        Scene scene = new Scene(mainLayout, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        logger.info("Fraud Detection UI started");
    }

    private BorderPane createMainLayout() {
        BorderPane layout = new BorderPane();

        // Top: Header and stats
        VBox header = createHeader();
        layout.setTop(header);

        // Center: Tables
        SplitPane centerPane = createCenterPane();
        layout.setCenter(centerPane);

        // Bottom: Status bar
        HBox statusBar = createStatusBar();
        layout.setBottom(statusBar);

        return layout;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");

        Label titleLabel = new Label("🔍 Real-Time Fraud Detection System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox statsBox = new HBox(30);
        statsBox.setPadding(new Insets(10, 0, 0, 0));

        transactionCountLabel = new Label("Transactions: 0");
        transactionCountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #3498db;");

        alertCountLabel = new Label("Fraud Alerts: 0");
        alertCountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c;");

        statusIndicator = new ProgressIndicator();
        statusIndicator.setMaxSize(20, 20);
        statusIndicator.setStyle("-fx-accent: #2ecc71;");

        Label statusText = new Label("System Status: ");
        statusText.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        statusLabel = new Label("Initializing...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f39c12;");

        HBox statusBox = new HBox(5, statusText, statusLabel, statusIndicator);

        statsBox.getChildren().addAll(transactionCountLabel, alertCountLabel, statusBox);
        header.getChildren().addAll(titleLabel, statsBox);

        return header;
    }

    private SplitPane createCenterPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.6);

        // Transaction table
        VBox transactionPane = createTransactionPane();

        // Alert table
        VBox alertPane = createAlertPane();

        splitPane.getItems().addAll(transactionPane, alertPane);

        return splitPane;
    }

    private VBox createTransactionPane() {
        VBox pane = new VBox(5);
        pane.setPadding(new Insets(10));

        Label titleLabel = new Label("📊 Live Transactions");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        transactionTable = new TableView<>();
        transactionTable.setItems(transactions);

        // Configure columns
        TableColumn<TransactionDisplayModel, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.setPrefWidth(80);

        TableColumn<TransactionDisplayModel, String> idCol = new TableColumn<>("Transaction ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        idCol.setPrefWidth(120);

        TableColumn<TransactionDisplayModel, String> accountCol = new TableColumn<>("Account");
        accountCol.setCellValueFactory(new PropertyValueFactory<>("accountId"));
        accountCol.setPrefWidth(80);

        TableColumn<TransactionDisplayModel, String> symbolCol = new TableColumn<>("Symbol");
        symbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        symbolCol.setPrefWidth(80);

        TableColumn<TransactionDisplayModel, String> sideCol = new TableColumn<>("Side");
        sideCol.setCellValueFactory(new PropertyValueFactory<>("side"));
        sideCol.setPrefWidth(60);

        TableColumn<TransactionDisplayModel, String> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(100);

        TableColumn<TransactionDisplayModel, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        TableColumn<TransactionDisplayModel, String> totalCol = new TableColumn<>("Total Value");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalValue"));
        totalCol.setPrefWidth(120);

        // Style side column
        sideCol.setCellFactory(column -> new TableCell<TransactionDisplayModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("BUY".equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        transactionTable.getColumns().addAll(timeCol, idCol, accountCol, symbolCol, sideCol, quantityCol, priceCol,
                totalCol);

        pane.getChildren().addAll(titleLabel, transactionTable);
        VBox.setVgrow(transactionTable, Priority.ALWAYS);

        return pane;
    }

    private VBox createAlertPane() {
        VBox pane = new VBox(5);
        pane.setPadding(new Insets(10));

        Label titleLabel = new Label("🚨 Fraud Alerts");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        alertTable = new TableView<>();
        alertTable.setItems(alerts);

        // Configure columns
        TableColumn<FraudAlertDisplayModel, String> timeCol = new TableColumn<>("Detected");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("detectedTime"));
        timeCol.setPrefWidth(80);

        TableColumn<FraudAlertDisplayModel, String> alertIdCol = new TableColumn<>("Alert ID");
        alertIdCol.setCellValueFactory(new PropertyValueFactory<>("alertId"));
        alertIdCol.setPrefWidth(120);

        TableColumn<FraudAlertDisplayModel, String> severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severityCol.setPrefWidth(80);

        TableColumn<FraudAlertDisplayModel, String> typeCol = new TableColumn<>("Fraud Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("fraudType"));
        typeCol.setPrefWidth(150);

        TableColumn<FraudAlertDisplayModel, String> accountCol = new TableColumn<>("Account");
        accountCol.setCellValueFactory(new PropertyValueFactory<>("accountId"));
        accountCol.setPrefWidth(80);

        TableColumn<FraudAlertDisplayModel, String> riskCol = new TableColumn<>("Risk Score");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("riskScore"));
        riskCol.setPrefWidth(80);

        TableColumn<FraudAlertDisplayModel, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(300);

        // Style severity column
        severityCol.setCellFactory(column -> new TableCell<FraudAlertDisplayModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "CRITICAL" -> setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                        case "HIGH" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "MEDIUM" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "LOW" -> setStyle("-fx-text-fill: #f1c40f;");
                    }
                }
            }
        });

        alertTable.getColumns().addAll(timeCol, alertIdCol, severityCol, typeCol, accountCol, riskCol, descCol);

        pane.getChildren().addAll(titleLabel, alertTable);
        VBox.setVgrow(alertTable, Priority.ALWAYS);

        return pane;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle("-fx-background-color: #34495e;");

        Label kafkaStatus = new Label("Kafka Status: Connected");
        kafkaStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timestamp = new Label(
                "Last Update: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timestamp.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px;");

        statusBar.getChildren().addAll(kafkaStatus, spacer, timestamp);

        return statusBar;
    }

    private void setupKafkaConsumers() {
        // Transaction consumer
        transactionConsumer = new KafkaConsumer<>(KafkaConfig.getConsumerProps("ui-transactions"));
        transactionConsumer.subscribe(Arrays.asList(KafkaConfig.TRANSACTIONS_TOPIC));

        // Alert consumer
        alertConsumer = new KafkaConsumer<>(KafkaConfig.getConsumerProps("ui-alerts"));
        alertConsumer.subscribe(Arrays.asList(KafkaConfig.FRAUD_ALERTS_TOPIC));

        // Start Kafka polling in background
        kafkaExecutor = Executors.newFixedThreadPool(2);

        kafkaExecutor.submit(this::consumeTransactions);
        kafkaExecutor.submit(this::consumeAlerts);

        Platform.runLater(() -> {
            statusLabel.setText("Running");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2ecc71;");
        });
    }

    private void consumeTransactions() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = transactionConsumer.poll(java.time.Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        Transaction transaction = objectMapper.readValue(record.value(), Transaction.class);

                        Platform.runLater(() -> {
                            TransactionDisplayModel displayModel = new TransactionDisplayModel(transaction);
                            transactions.add(0, displayModel); // Add at top

                            // Keep only last 100 transactions
                            if (transactions.size() > 100) {
                                transactions.remove(transactions.size() - 1);
                            }

                            transactionCountLabel.setText("Transactions: " + transactionCount.incrementAndGet());
                        });

                    } catch (Exception e) {
                        logger.error("Error parsing transaction", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in transaction consumer", e);
        }
    }

    private void consumeAlerts() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = alertConsumer.poll(java.time.Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        FraudAlert alert = objectMapper.readValue(record.value(), FraudAlert.class);

                        Platform.runLater(() -> {
                            FraudAlertDisplayModel displayModel = new FraudAlertDisplayModel(alert);
                            alerts.add(0, displayModel); // Add at top

                            // Keep only last 50 alerts
                            if (alerts.size() > 50) {
                                alerts.remove(alerts.size() - 1);
                            }

                            alertCountLabel.setText("Fraud Alerts: " + alertCount.incrementAndGet());

                            // Flash the alert count for attention
                            alertCountLabel.setStyle(
                                    "-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-background-color: #f8d7da; -fx-padding: 2px 6px; -fx-background-radius: 3px;");

                            // Reset style after 2 seconds
                            Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(2), e -> {
                                alertCountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c;");
                            }));
                            timeline.play();
                        });

                    } catch (Exception e) {
                        logger.error("Error parsing fraud alert", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in alert consumer", e);
        }
    }

    private void cleanup() {
        logger.info("Shutting down Fraud Detection UI...");

        if (kafkaExecutor != null) {
            kafkaExecutor.shutdownNow();
        }

        if (transactionConsumer != null) {
            transactionConsumer.close();
        }

        if (alertConsumer != null) {
            alertConsumer.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Display Models
    public static class TransactionDisplayModel {
        private final String time;
        private final String transactionId;
        private final String accountId;
        private final String symbol;
        private final String side;
        private final String quantity;
        private final String price;
        private final String totalValue;

        public TransactionDisplayModel(Transaction transaction) {
            this.time = transaction.getTimestamp().format(TIME_FORMATTER);
            this.transactionId = transaction.getTransactionId();
            this.accountId = transaction.getAccountId();
            this.symbol = transaction.getSymbol();
            this.side = transaction.getSide();
            this.quantity = transaction.getQuantity().toString();
            this.price = "$" + transaction.getPrice().toString();
            this.totalValue = "$" + transaction.getTotalValue().toString();
        }

        // Getters (required for TableView)
        public String getTime() {
            return time;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getSide() {
            return side;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getPrice() {
            return price;
        }

        public String getTotalValue() {
            return totalValue;
        }
    }

    public static class FraudAlertDisplayModel {
        private final String detectedTime;
        private final String alertId;
        private final String severity;
        private final String fraudType;
        private final String accountId;
        private final String riskScore;
        private final String description;

        public FraudAlertDisplayModel(FraudAlert alert) {
            this.detectedTime = alert.getDetectedAt().format(TIME_FORMATTER);
            this.alertId = alert.getAlertId();
            this.severity = alert.getSeverity().toString();
            this.fraudType = alert.getFraudType().toString();
            this.accountId = alert.getAccountId();
            this.riskScore = alert.getRiskScore().multiply(new BigDecimal("100")).setScale(1,
                    java.math.RoundingMode.HALF_UP) + "%";
            this.description = alert.getDescription();
        }

        // Getters (required for TableView)
        public String getDetectedTime() {
            return detectedTime;
        }

        public String getAlertId() {
            return alertId;
        }

        public String getSeverity() {
            return severity;
        }

        public String getFraudType() {
            return fraudType;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getRiskScore() {
            return riskScore;
        }

        public String getDescription() {
            return description;
        }
    }
}