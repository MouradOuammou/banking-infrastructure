package com.banking.accounts.dto;

import com.banking.accounts.entity.AccountStatus;
import com.banking.accounts.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountDto {
    private Long id;
    private String accountNumber;
    private Long userId;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal overdraftLimit;
    private String currency;
    private AccountStatus status;
    private String branchCode;
    private String iban;
    private String bic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public AccountDto() {}

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
}
