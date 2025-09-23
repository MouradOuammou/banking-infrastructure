package com.banking.transactions.controller;

import com.banking.transactions.dto.*;
import com.banking.transactions.entity.TransactionStatus;
import com.banking.transactions.entity.TransactionType;
import com.banking.transactions.service.TransactionService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "transactions.transfer", description = "Time taken to process transfer")
    public ResponseEntity<TransactionDto> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionDto transaction = transactionService.transfer(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "transactions.deposit", description = "Time taken to process deposit")
    public ResponseEntity<TransactionDto> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionDto transaction = transactionService.deposit(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @PostMapping("/withdrawal")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "transactions.withdrawal", description = "Time taken to process withdrawal")
    public ResponseEntity<TransactionDto> withdrawal(@Valid @RequestBody WithdrawalRequest request) {
        TransactionDto transaction = transactionService.withdrawal(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "transactions.get", description = "Time taken to get transaction")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(transaction -> ResponseEntity.ok(transaction))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{transactionId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<TransactionDto> getTransactionByReference(@PathVariable String transactionId) {
        return transactionService.getTransactionByTransactionId(transactionId)
                .map(transaction -> ResponseEntity.ok(transaction))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "transactions.getByAccount", description = "Time taken to get transactions by account")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByAccount(
            @PathVariable String accountNumber, 
            Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getTransactionsByAccountNumber(accountNumber, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByUser(
            @PathVariable Long userId, 
            Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getTransactionsByUserId(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountNumber}/type/{type}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByAccountAndType(
            @PathVariable String accountNumber,
            @PathVariable TransactionType type,
            Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getTransactionsByAccountAndType(accountNumber, type, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountNumber}/date-range")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByAccountAndDateRange(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getTransactionsByAccountAndDateRange(
                accountNumber, startDate, endDate, pageable);
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "transactions.updateStatus", description = "Time taken to update transaction status")
    public ResponseEntity<TransactionDto> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status,
            @RequestParam(required = false) String reason) {
        TransactionDto transaction = transactionService.updateTransactionStatus(id, status, reason);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/account/{accountNumber}/balance")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getAccountBalance(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        BigDecimal balance = transactionService.getAccountBalance(accountNumber, startDate, endDate);
        return ResponseEntity.ok(balance);
    }
}
