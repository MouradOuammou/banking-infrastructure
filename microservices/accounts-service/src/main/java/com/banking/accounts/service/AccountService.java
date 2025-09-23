package com.banking.accounts.service;

import com.banking.accounts.dto.AccountDto;
import com.banking.accounts.dto.CreateAccountRequest;
import com.banking.accounts.dto.UpdateBalanceRequest;
import com.banking.accounts.entity.Account;
import com.banking.accounts.entity.AccountStatus;
import com.banking.accounts.entity.AccountType;
import com.banking.accounts.exception.AccountNotFoundException;
import com.banking.accounts.exception.InsufficientFundsException;
import com.banking.accounts.mapper.AccountMapper;
import com.banking.accounts.repository.AccountRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @CircuitBreaker(name = "account-service", fallbackMethod = "createAccountFallback")
    @Retry(name = "account-service")
    public AccountDto createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setUserId(request.getUserId());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getInitialDeposit());
        account.setOverdraftLimit(request.getOverdraftLimit());
        account.setCurrency(request.getCurrency());
        account.setBranchCode(request.getBranchCode());
        account.setIban(generateIban(account.getAccountNumber()));
        account.setBic("BANKFRPP");
        account.updateAvailableBalance();

        Account savedAccount = accountRepository.save(account);

        // Send account creation event
        sendAccountCreatedEvent(savedAccount);

        return accountMapper.toDto(savedAccount);
    }

    public AccountDto createAccountFallback(CreateAccountRequest request, Exception ex) {
        logger.error("Failed to create account: {}", ex.getMessage());
        throw new RuntimeException("Account creation service is currently unavailable");
    }

    @Transactional(readOnly = true)
    public Optional<AccountDto> getAccountById(Long id) {
        return accountRepository.findById(id)
                .map(accountMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<AccountDto> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(accountMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AccountDto> getAccountsByUserId(Long userId, Pageable pageable) {
        return accountRepository.findByUserId(userId,pageable)
                .map(accountMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getActiveAccountsByUserId(Long userId) {
        return accountRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
                .stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @CircuitBreaker(name = "account-service", fallbackMethod = "updateBalanceFallback")
    @Retry(name = "account-service")
    public AccountDto updateBalance(Long accountId, UpdateBalanceRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update balance for inactive account");
        }

        BigDecimal amount = request.getAmount();
        String operation = request.getOperation().toUpperCase();

        switch (operation) {
            case "CREDIT":
                account.credit(amount);
                break;
            case "DEBIT":
                if (!account.canDebit(amount)) {
                    throw new InsufficientFundsException("Insufficient funds for debit operation");
                }
                account.debit(amount);
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + operation);
        }

        Account updatedAccount = accountRepository.save(account);

        // Send balance update event
        sendBalanceUpdatedEvent(updatedAccount, request);

        return accountMapper.toDto(updatedAccount);
    }

    public AccountDto updateBalanceFallback(Long accountId, UpdateBalanceRequest request, Exception ex) {
        logger.error("Failed to update balance for account {}: {}", accountId, ex.getMessage());
        throw new RuntimeException("Balance update service is currently unavailable");
    }

    public AccountDto updateAccountStatus(Long accountId, AccountStatus status) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        account.setStatus(status);
        Account updatedAccount = accountRepository.save(account);

        // Send status update event
        sendAccountStatusUpdatedEvent(updatedAccount);

        return accountMapper.toDto(updatedAccount);
    }

    @Transactional(readOnly = true)
    public Long countActiveAccountsByUserId(Long userId) {
        return accountRepository.countActiveAccountsByUserId(userId);
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        String accountNumber = sb.toString();

        // Ensure uniqueness
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            return generateAccountNumber();
        }

        return accountNumber;
    }

    private String generateIban(String accountNumber) {
        // Simplified IBAN generation for France (FR)
        return "FR76" + "30003" + "00001" + accountNumber;
    }

    private void sendAccountCreatedEvent(Account account) {
        try {
            AccountCreatedEvent event = new AccountCreatedEvent(
                account.getId(),
                account.getAccountNumber(),
                account.getUserId(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getCurrency()
            );
            kafkaTemplate.send("account-events", event);
            logger.info("Account created event sent for account: {}", account.getAccountNumber());
        } catch (Exception e) {
            logger.error("Failed to send account created event: {}", e.getMessage());
        }
    }

    private void sendBalanceUpdatedEvent(Account account, UpdateBalanceRequest request) {
        try {
            BalanceUpdatedEvent event = new BalanceUpdatedEvent(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getAvailableBalance(),
                request.getOperation(),
                request.getAmount(),
                request.getTransactionId()
            );
            kafkaTemplate.send("account-events", event);
            logger.info("Balance updated event sent for account: {}", account.getAccountNumber());
        } catch (Exception e) {
            logger.error("Failed to send balance updated event: {}", e.getMessage());
        }
    }

    private void sendAccountStatusUpdatedEvent(Account account) {
        try {
            AccountStatusUpdatedEvent event = new AccountStatusUpdatedEvent(
                account.getId(),
                account.getAccountNumber(),
                account.getStatus().name()
            );
            kafkaTemplate.send("account-events", event);
            logger.info("Account status updated event sent for account: {}", account.getAccountNumber());
        } catch (Exception e) {
            logger.error("Failed to send account status updated event: {}", e.getMessage());
        }
    }

    // Event classes
    public static class AccountCreatedEvent {
        private Long accountId;
        private String accountNumber;
        private Long userId;
        private String accountType;
        private BigDecimal balance;
        private String currency;

        public AccountCreatedEvent(Long accountId, String accountNumber, Long userId, 
                                 String accountType, BigDecimal balance, String currency) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.userId = userId;
            this.accountType = accountType;
            this.balance = balance;
            this.currency = currency;
        }

        // Getters and setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class BalanceUpdatedEvent {
        private Long accountId;
        private String accountNumber;
        private BigDecimal newBalance;
        private BigDecimal availableBalance;
        private String operation;
        private BigDecimal amount;
        private String transactionId;

        public BalanceUpdatedEvent(Long accountId, String accountNumber, BigDecimal newBalance,
                                 BigDecimal availableBalance, String operation, BigDecimal amount, String transactionId) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.newBalance = newBalance;
            this.availableBalance = availableBalance;
            this.operation = operation;
            this.amount = amount;
            this.transactionId = transactionId;
        }

        // Getters and setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public BigDecimal getNewBalance() { return newBalance; }
        public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }

        public BigDecimal getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }

    public static class AccountStatusUpdatedEvent {
        private Long accountId;
        private String accountNumber;
        private String status;

        public AccountStatusUpdatedEvent(Long accountId, String accountNumber, String status) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.status = status;
        }

        // Getters and setters
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
