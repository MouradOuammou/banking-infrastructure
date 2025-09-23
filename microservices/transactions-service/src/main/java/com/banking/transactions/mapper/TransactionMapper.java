package com.banking.transactions.mapper;

import com.banking.transactions.dto.TransactionDto;
import com.banking.transactions.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDto toDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionId(transaction.getTransactionId());
        dto.setType(transaction.getType());
        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency());
        dto.setFromAccountNumber(transaction.getFromAccountNumber());
        dto.setToAccountNumber(transaction.getToAccountNumber());
        dto.setFromUserId(transaction.getFromUserId());
        dto.setToUserId(transaction.getToUserId());
        dto.setDescription(transaction.getDescription());
        dto.setReference(transaction.getReference());
        dto.setStatus(transaction.getStatus());
        dto.setStatusReason(transaction.getStatusReason());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setProcessedDate(transaction.getProcessedDate());
        dto.setFees(transaction.getFees());
        dto.setExchangeRate(transaction.getExchangeRate());
        dto.setMerchantName(transaction.getMerchantName());
        dto.setMerchantCategory(transaction.getMerchantCategory());
        dto.setLocation(transaction.getLocation());

        return dto;
    }

    public Transaction toEntity(TransactionDto dto) {
        if (dto == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setId(dto.getId());
        transaction.setTransactionId(dto.getTransactionId());
        transaction.setType(dto.getType());
        transaction.setAmount(dto.getAmount());
        transaction.setCurrency(dto.getCurrency());
        transaction.setFromAccountNumber(dto.getFromAccountNumber());
        transaction.setToAccountNumber(dto.getToAccountNumber());
        transaction.setFromUserId(dto.getFromUserId());
        transaction.setToUserId(dto.getToUserId());
        transaction.setDescription(dto.getDescription());
        transaction.setReference(dto.getReference());
        transaction.setStatus(dto.getStatus());
        transaction.setStatusReason(dto.getStatusReason());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setProcessedDate(dto.getProcessedDate());
        transaction.setFees(dto.getFees());
        transaction.setExchangeRate(dto.getExchangeRate());
        transaction.setMerchantName(dto.getMerchantName());
        transaction.setMerchantCategory(dto.getMerchantCategory());
        transaction.setLocation(dto.getLocation());

        return transaction;
    }
}
