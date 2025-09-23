package com.banking.transactions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class WithdrawalRequest {
    @NotBlank
    private String accountNumber;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String currency = "EUR";

    private String description;

    private String reference;

    private String location;

    // Getters and Setters
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
