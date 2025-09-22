package com.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class FraudAlert {
    private String alertId;
    private String transactionId;
    private String accountId;
    private FraudType fraudType;
    private String description;
    private SeverityLevel severity;
    private BigDecimal riskScore; // 0.0 to 1.0
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime detectedAt;
    
    private Transaction suspiciousTransaction;
    private List<String> triggeredRules;
    private String recommendedAction;

    // Default constructor for Jackson
    public FraudAlert() {}

    public FraudAlert(String alertId, String transactionId, String accountId, 
                     FraudType fraudType, String description, SeverityLevel severity,
                     BigDecimal riskScore, Transaction transaction, List<String> triggeredRules) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.fraudType = fraudType;
        this.description = description;
        this.severity = severity;
        this.riskScore = riskScore;
        this.detectedAt = LocalDateTime.now();
        this.suspiciousTransaction = transaction;
        this.triggeredRules = triggeredRules;
        this.recommendedAction = determineRecommendedAction(severity, riskScore);
    }

    private String determineRecommendedAction(SeverityLevel severity, BigDecimal riskScore) {
        if (severity == SeverityLevel.CRITICAL || riskScore.compareTo(new BigDecimal("0.8")) >= 0) {
            return "BLOCK_TRANSACTION";
        } else if (severity == SeverityLevel.HIGH || riskScore.compareTo(new BigDecimal("0.6")) >= 0) {
            return "MANUAL_REVIEW";
        } else {
            return "MONITOR";
        }
    }

    // Getters and Setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public FraudType getFraudType() { return fraudType; }
    public void setFraudType(FraudType fraudType) { this.fraudType = fraudType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SeverityLevel getSeverity() { return severity; }
    public void setSeverity(SeverityLevel severity) { this.severity = severity; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }

    public Transaction getSuspiciousTransaction() { return suspiciousTransaction; }
    public void setSuspiciousTransaction(Transaction suspiciousTransaction) { 
        this.suspiciousTransaction = suspiciousTransaction; 
    }

    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public enum FraudType {
        HIGH_VOLUME, RAPID_TRADING, OFF_HOURS_TRADING, PRICE_MANIPULATION, 
        UNUSUAL_PATTERN, ACCOUNT_TAKEOVER, PUMP_AND_DUMP
    }

    public enum SeverityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FraudAlert that = (FraudAlert) o;
        return Objects.equals(alertId, that.alertId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertId);
    }

    @Override
    public String toString() {
        return String.format("FraudAlert{id=%s, type=%s, severity=%s, score=%s, account=%s, description='%s'}", 
            alertId, fraudType, severity, riskScore, accountId, description);
    }
}