package com.banking.transactions.service;

import com.banking.transactions.dto.TransferRequest;
import com.banking.transactions.dto.DepositRequest;
import com.banking.transactions.dto.WithdrawalRequest;
import com.banking.transactions.entity.Transaction;
import com.banking.transactions.entity.TransactionStatus;
import com.banking.transactions.entity.TransactionType;
import com.banking.transactions.mapper.TransactionMapper;
import com.banking.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction testTransaction;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setTransactionId("TXN123456789");
        testTransaction.setType(TransactionType.TRANSFER);
        testTransaction.setAmount(new BigDecimal("1000.00"));
        testTransaction.setFromAccountNumber("1234567890");
        testTransaction.setToAccountNumber("0987654321");
        testTransaction.setStatus(TransactionStatus.PENDING);

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber("1234567890");
        transferRequest.setToAccountNumber("0987654321");
        transferRequest.setAmount(new BigDecimal("1000.00"));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    void testTransferCreation() {
        // Given
        when(transactionRepository.existsByTransactionId(any())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        transactionService.transfer(transferRequest);

        // Then
        verify(transactionRepository).save(any(Transaction.class));
        verify(kafkaTemplate, never()).send(eq("transaction-events"), any()); // Event sent asynchronously
    }

    @Test
    void testDepositCreation() {
        // Given
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAccountNumber("1234567890");
        depositRequest.setAmount(new BigDecimal("500.00"));
        depositRequest.setDescription("Test deposit");

        Transaction depositTransaction = new Transaction();
        depositTransaction.setId(2L);
        depositTransaction.setTransactionId("TXN123456790");
        depositTransaction.setType(TransactionType.DEPOSIT);
        depositTransaction.setAmount(new BigDecimal("500.00"));
        depositTransaction.setToAccountNumber("1234567890");
        depositTransaction.setStatus(TransactionStatus.PENDING);

        when(transactionRepository.existsByTransactionId(any())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(depositTransaction);

        // When
        transactionService.deposit(depositRequest);

        // Then
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdrawalCreation() {
        // Given
        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAccountNumber("1234567890");
        withdrawalRequest.setAmount(new BigDecimal("300.00"));
        withdrawalRequest.setDescription("Test withdrawal");

        Transaction withdrawalTransaction = new Transaction();
        withdrawalTransaction.setId(3L);
        withdrawalTransaction.setTransactionId("TXN123456791");
        withdrawalTransaction.setType(TransactionType.WITHDRAWAL);
        withdrawalTransaction.setAmount(new BigDecimal("300.00"));
        withdrawalTransaction.setFromAccountNumber("1234567890");
        withdrawalTransaction.setStatus(TransactionStatus.PENDING);

        when(transactionRepository.existsByTransactionId(any())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(withdrawalTransaction);

        // When
        transactionService.withdrawal(withdrawalRequest);

        // Then
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testUpdateTransactionStatus() {
        // Given
        when(transactionRepository.findById(1L)).thenReturn(java.util.Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        transactionService.updateTransactionStatus(1L, TransactionStatus.COMPLETED, "Transaction completed successfully");

        // Then
        assertEquals(TransactionStatus.COMPLETED, testTransaction.getStatus());
        assertEquals("Transaction completed successfully", testTransaction.getStatusReason());
        assertNotNull(testTransaction.getProcessedDate());
        verify(transactionRepository).save(testTransaction);
        verify(kafkaTemplate).send(eq("transaction-events"), any());
    }
}
