package com.banking.accounts.dto;

import com.banking.accounts.entity.AccountType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateAccountRequest {
    @NotNull
    private Long userId;

    @NotNull
    private AccountType accountType;

    private BigDecimal initialDeposit = BigDecimal.ZERO;

    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    private String currency = "EUR";

    private String branchCode;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public BigDecimal getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(BigDecimal initialDeposit) { this.initialDeposit = initialDeposit; }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(BigDecimal overdraftLimit) { this.overdraftLimit = overdraftLimit; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
}
