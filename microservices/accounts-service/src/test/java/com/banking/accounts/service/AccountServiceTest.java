package com.banking.accounts.service;

import com.banking.accounts.dto.CreateAccountRequest;
import com.banking.accounts.dto.UpdateBalanceRequest;
import com.banking.accounts.entity.Account;
import com.banking.accounts.entity.AccountStatus;
import com.banking.accounts.entity.AccountType;
import com.banking.accounts.exception.AccountNotFoundException;
import com.banking.accounts.exception.InsufficientFundsException;
import com.banking.accounts.mapper.AccountMapper;
import com.banking.accounts.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private CreateAccountRequest createRequest;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("1234567890");
        testAccount.setUserId(1L);
        testAccount.setAccountType(AccountType.CHECKING);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setOverdraftLimit(new BigDecimal("500.00"));
        testAccount.updateAvailableBalance();
        testAccount.setStatus(AccountStatus.ACTIVE);

        createRequest = new CreateAccountRequest();
        createRequest.setUserId(1L);
        createRequest.setAccountType(AccountType.CHECKING);
        createRequest.setInitialDeposit(new BigDecimal("1000.00"));
    }

    @Test
    void testCreateAccountSuccess() {
        // Given
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        accountService.createAccount(createRequest);

        // Then
        verify(accountRepository).save(any(Account.class));
        verify(kafkaTemplate).send(eq("account-events"), any());
    }

    @Test
    void testUpdateBalanceCredit() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setOperation("CREDIT");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        accountService.updateBalance(1L, request);

        // Then
        assertEquals(new BigDecimal("1500.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
        verify(kafkaTemplate).send(eq("account-events"), any());
    }

    @Test
    void testUpdateBalanceDebitSuccess() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setOperation("DEBIT");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        accountService.updateBalance(1L, request);

        // Then
        assertEquals(new BigDecimal("500.00"), testAccount.getBalance());
        verify(accountRepository).save(testAccount);
    }

    @Test
    void testUpdateBalanceDebitInsufficientFunds() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAmount(new BigDecimal("2000.00")); // More than available balance
        request.setOperation("DEBIT");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThrows(InsufficientFundsException.class, () -> {
            accountService.updateBalance(1L, request);
        });
    }

    @Test
    void testUpdateBalanceAccountNotFound() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setOperation("CREDIT");

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.updateBalance(1L, request);
        });
    }

    @Test
    void testUpdateAccountStatus() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        accountService.updateAccountStatus(1L, AccountStatus.SUSPENDED);

        // Then
        assertEquals(AccountStatus.SUSPENDED, testAccount.getStatus());
        verify(accountRepository).save(testAccount);
        verify(kafkaTemplate).send(eq("account-events"), any());
    }
}
