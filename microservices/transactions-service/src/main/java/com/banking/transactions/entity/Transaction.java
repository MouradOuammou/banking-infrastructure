package com.banking.transactions.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_from_account", columnList = "fromAccountNumber"),
    @Index(name = "idx_to_account", columnList = "toAccountNumber"),
    @Index(name = "idx_transaction_date", columnList = "transactionDate"),
    @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, length = 50)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "EUR";

    private String fromAccountNumber;

    private String toAccountNumber;

    private Long fromUserId;

    private Long toUserId;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 1000)
    private String statusReason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime transactionDate;

    private LocalDateTime processedDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal fees = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal exchangeRate;

    private String merchantName;

    private String merchantCategory;

    private String location;

    // Constructors
    public Transaction() {}

    public Transaction(String transactionId, TransactionType type, BigDecimal amount, 
                      String fromAccountNumber, String toAccountNumber) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getFromAccountNumber() { return fromAccountNumber; }
    public void setFromAccountNumber(String fromAccountNumber) { this.fromAccountNumber = fromAccountNumber; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public LocalDateTime getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }

    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // Business methods
    public boolean isTransfer() {
        return type == TransactionType.TRANSFER;
    }

    public boolean isDeposit() {
        return type == TransactionType.DEPOSIT;
    }

    public boolean isWithdrawal() {
        return type == TransactionType.WITHDRAWAL;
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }
}
