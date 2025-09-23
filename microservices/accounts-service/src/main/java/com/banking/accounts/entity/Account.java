package com.banking.accounts.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = "accountNumber")
})
@EntityListeners(AuditingEntityListener.class)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, length = 20)
    private String accountNumber;

    @NotNull
    private Long userId;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 2)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column(length = 3)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    private String branchCode;

    private String iban;

    private String bic;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public Account() {}

    public Account(String accountNumber, Long userId, AccountType accountType) {
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.accountType = accountType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(BigDecimal overdraftLimit) { this.overdraftLimit = overdraftLimit; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business methods
    public void updateAvailableBalance() {
        this.availableBalance = this.balance.add(this.overdraftLimit);
    }

    public boolean canDebit(BigDecimal amount) {
        return this.availableBalance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!canDebit(amount)) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
        updateAvailableBalance();
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        updateAvailableBalance();
    }
}
