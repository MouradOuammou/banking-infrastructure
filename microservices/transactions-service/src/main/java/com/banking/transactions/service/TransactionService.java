package com.banking.transactions.service;

import com.banking.transactions.dto.*;
import com.banking.transactions.entity.Transaction;
import com.banking.transactions.entity.TransactionStatus;
import com.banking.transactions.entity.TransactionType;
import com.banking.transactions.exception.AccountNotFoundException;
import com.banking.transactions.exception.InsufficientFundsException;
import com.banking.transactions.exception.TransactionNotFoundException;
import com.banking.transactions.mapper.TransactionMapper;
import com.banking.transactions.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "transferFallback")
    @Retry(name = "transaction-service")
    public TransactionDto transfer(TransferRequest request) {
        // Validate accounts exist and have sufficient funds
        validateTransferRequest(request);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setFromAccountNumber(request.getFromAccountNumber());
        transaction.setToAccountNumber(request.getToAccountNumber());
        transaction.setDescription(request.getDescription());
        transaction.setReference(request.getReference());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Process the transfer asynchronously
        processTransfer(savedTransaction);

        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionDto transferFallback(TransferRequest request, Exception ex) {
        logger.error("Failed to process transfer: {}", ex.getMessage());
        throw new RuntimeException("Transfer service is currently unavailable");
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "depositFallback")
    @Retry(name = "transaction-service")
    public TransactionDto deposit(DepositRequest request) {
        // Validate account exists
        validateAccountExists(request.getAccountNumber());

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setToAccountNumber(request.getAccountNumber());
        transaction.setDescription(request.getDescription());
        transaction.setReference(request.getReference());
        transaction.setMerchantName(request.getMerchantName());
        transaction.setLocation(request.getLocation());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Process the deposit
        processDeposit(savedTransaction);

        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionDto depositFallback(DepositRequest request, Exception ex) {
        logger.error("Failed to process deposit: {}", ex.getMessage());
        throw new RuntimeException("Deposit service is currently unavailable");
    }

    @CircuitBreaker(name = "transaction-service", fallbackMethod = "withdrawalFallback")
    @Retry(name = "transaction-service")
    public TransactionDto withdrawal(WithdrawalRequest request) {
        // Validate account exists and has sufficient funds
        validateWithdrawalRequest(request);

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setFromAccountNumber(request.getAccountNumber());
        transaction.setDescription(request.getDescription());
        transaction.setReference(request.getReference());
        transaction.setLocation(request.getLocation());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Process the withdrawal
        processWithdrawal(savedTransaction);

        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionDto withdrawalFallback(WithdrawalRequest request, Exception ex) {
        logger.error("Failed to process withdrawal: {}", ex.getMessage());
        throw new RuntimeException("Withdrawal service is currently unavailable");
    }

    @Transactional(readOnly = true)
    public Optional<TransactionDto> getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<TransactionDto> getTransactionByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByAccountNumber(String accountNumber, Pageable pageable) {
        return transactionRepository.findByAccountNumber(accountNumber, pageable)
                .map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByUserId(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByAccountAndType(String accountNumber, TransactionType type, Pageable pageable) {
        return transactionRepository.findByAccountNumberAndType(accountNumber, type, pageable)
                .map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByAccountAndDateRange(String accountNumber, 
                                                                   LocalDateTime startDate, 
                                                                   LocalDateTime endDate, 
                                                                   Pageable pageable) {
        return transactionRepository.findByAccountNumberAndDateRange(accountNumber, startDate, endDate, pageable)
                .map(transactionMapper::toDto);
    }

    public TransactionDto updateTransactionStatus(Long transactionId, TransactionStatus status, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + transactionId));

        transaction.setStatus(status);
        transaction.setStatusReason(reason);
        
        if (status == TransactionStatus.COMPLETED || status == TransactionStatus.FAILED) {
            transaction.setProcessedDate(LocalDateTime.now());
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);

        // Send status update event
        sendTransactionStatusUpdatedEvent(updatedTransaction);

        return transactionMapper.toDto(updatedTransaction);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(String accountNumber, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal incoming = transactionRepository.sumIncomingAmountByAccountAndDateRange(accountNumber, startDate, endDate);
        BigDecimal outgoing = transactionRepository.sumOutgoingAmountByAccountAndDateRange(accountNumber, startDate, endDate);
        
        if (incoming == null) incoming = BigDecimal.ZERO;
        if (outgoing == null) outgoing = BigDecimal.ZERO;
        
        return incoming.subtract(outgoing);
    }

    private void processTransfer(Transaction transaction) {
        try {
            // Update from account (debit)
            updateAccountBalance(transaction.getFromAccountNumber(), transaction.getAmount().negate(), 
                               "DEBIT", transaction.getTransactionId());

            // Update to account (credit)
            updateAccountBalance(transaction.getToAccountNumber(), transaction.getAmount(), 
                               "CREDIT", transaction.getTransactionId());

            // Update transaction status
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send completion event
            sendTransactionCompletedEvent(transaction);

        } catch (Exception e) {
            logger.error("Failed to process transfer {}: {}", transaction.getTransactionId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setStatusReason(e.getMessage());
            transaction.setProcessedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            sendTransactionFailedEvent(transaction);
        }
    }

    private void processDeposit(Transaction transaction) {
        try {
            // Update account (credit)
            updateAccountBalance(transaction.getToAccountNumber(), transaction.getAmount(), 
                               "CREDIT", transaction.getTransactionId());

            // Update transaction status
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send completion event
            sendTransactionCompletedEvent(transaction);

        } catch (Exception e) {
            logger.error("Failed to process deposit {}: {}", transaction.getTransactionId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setStatusReason(e.getMessage());
            transaction.setProcessedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            sendTransactionFailedEvent(transaction);
        }
    }

    private void processWithdrawal(Transaction transaction) {
        try {
            // Update account (debit)
            updateAccountBalance(transaction.getFromAccountNumber(), transaction.getAmount().negate(), 
                               "DEBIT", transaction.getTransactionId());

            // Update transaction status
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send completion event
            sendTransactionCompletedEvent(transaction);

        } catch (Exception e) {
            logger.error("Failed to process withdrawal {}: {}", transaction.getTransactionId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setStatusReason(e.getMessage());
            transaction.setProcessedDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            sendTransactionFailedEvent(transaction);
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        // Validate accounts exist
        validateAccountExists(request.getFromAccountNumber());
        validateAccountExists(request.getToAccountNumber());

        // Validate sufficient funds
        validateSufficientFunds(request.getFromAccountNumber(), request.getAmount());
    }

    private void validateWithdrawalRequest(WithdrawalRequest request) {
        // Validate account exists
        validateAccountExists(request.getAccountNumber());

        // Validate sufficient funds
        validateSufficientFunds(request.getAccountNumber(), request.getAmount());
    }

    private void validateAccountExists(String accountNumber) {
        try {
            webClientBuilder.build()
                    .get()
                    .uri("http://accounts-service/api/accounts/number/" + accountNumber)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
    }

    private void validateSufficientFunds(String accountNumber, BigDecimal amount) {
        // This would typically call the accounts service to check available balance
        // For now, we'll assume the accounts service handles this validation
    }

    private void updateAccountBalance(String accountNumber, BigDecimal amount, String operation, String transactionId) {
        try {
            UpdateBalanceRequest request = new UpdateBalanceRequest();
            request.setAmount(amount.abs());
            request.setOperation(operation);
            request.setTransactionId(transactionId);

            webClientBuilder.build()
                    .put()
                    .uri("http://accounts-service/api/accounts/number/" + accountNumber + "/balance")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update account balance: " + e.getMessage());
        }
    }

    private String generateTransactionId() {
        String transactionId = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Ensure uniqueness
        if (transactionRepository.existsByTransactionId(transactionId)) {
            return generateTransactionId();
        }
        
        return transactionId;
    }

    private void sendTransactionCompletedEvent(Transaction transaction) {
        try {
            TransactionCompletedEvent event = new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                transaction.getProcessedDate()
            );
            kafkaTemplate.send("transaction-events", event);
            logger.info("Transaction completed event sent for: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to send transaction completed event: {}", e.getMessage());
        }
    }

    private void sendTransactionFailedEvent(Transaction transaction) {
        try {
            TransactionFailedEvent event = new TransactionFailedEvent(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getStatusReason(),
                transaction.getProcessedDate()
            );
            kafkaTemplate.send("transaction-events", event);
            logger.info("Transaction failed event sent for: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to send transaction failed event: {}", e.getMessage());
        }
    }

    private void sendTransactionStatusUpdatedEvent(Transaction transaction) {
        try {
            TransactionStatusUpdatedEvent event = new TransactionStatusUpdatedEvent(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getStatus().name(),
                transaction.getStatusReason()
            );
            kafkaTemplate.send("transaction-events", event);
            logger.info("Transaction status updated event sent for: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to send transaction status updated event: {}", e.getMessage());
        }
    }

    // Event classes
    public static class TransactionCompletedEvent {
        private Long transactionId;
        private String transactionReference;
        private String type;
        private BigDecimal amount;
        private String fromAccount;
        private String toAccount;
        private LocalDateTime completedAt;

        public TransactionCompletedEvent(Long transactionId, String transactionReference, String type,
                                       BigDecimal amount, String fromAccount, String toAccount, LocalDateTime completedAt) {
            this.transactionId = transactionId;
            this.transactionReference = transactionReference;
            this.type = type;
            this.amount = amount;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.completedAt = completedAt;
        }

        // Getters and setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getFromAccount() { return fromAccount; }
        public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

        public String getToAccount() { return toAccount; }
        public void setToAccount(String toAccount) { this.toAccount = toAccount; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }

    public static class TransactionFailedEvent {
        private Long transactionId;
        private String transactionReference;
        private String type;
        private BigDecimal amount;
        private String reason;
        private LocalDateTime failedAt;

        public TransactionFailedEvent(Long transactionId, String transactionReference, String type,
                                    BigDecimal amount, String reason, LocalDateTime failedAt) {
            this.transactionId = transactionId;
            this.transactionReference = transactionReference;
            this.type = type;
            this.amount = amount;
            this.reason = reason;
            this.failedAt = failedAt;
        }

        // Getters and setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public LocalDateTime getFailedAt() { return failedAt; }
        public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    }

    public static class TransactionStatusUpdatedEvent {
        private Long transactionId;
        private String transactionReference;
        private String status;
        private String reason;

        public TransactionStatusUpdatedEvent(Long transactionId, String transactionReference, String status, String reason) {
            this.transactionId = transactionId;
            this.transactionReference = transactionReference;
            this.status = status;
            this.reason = reason;
        }

        // Getters and setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // DTO for account service communication
    public static class UpdateBalanceRequest {
        private BigDecimal amount;
        private String operation;
        private String transactionId;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }
}
