package com.banking.transactions.repository;

import com.banking.transactions.entity.Transaction;
import com.banking.transactions.entity.TransactionStatus;
import com.banking.transactions.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber")
    Page<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromUserId = :userId OR t.toUserId = :userId)")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber) AND t.status = :status")
    List<Transaction> findByAccountNumberAndStatus(@Param("accountNumber") String accountNumber, 
                                                  @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber) AND t.type = :type")
    Page<Transaction> findByAccountNumberAndType(@Param("accountNumber") String accountNumber, 
                                               @Param("type") TransactionType type, 
                                               Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber) AND t.transactionDate BETWEEN :startDate AND :endDate")
    Page<Transaction> findByAccountNumberAndDateRange(@Param("accountNumber") String accountNumber,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.fromAccountNumber = :accountNumber AND t.status = 'COMPLETED' AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumOutgoingAmountByAccountAndDateRange(@Param("accountNumber") String accountNumber,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.toAccountNumber = :accountNumber AND t.status = 'COMPLETED' AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumIncomingAmountByAccountAndDateRange(@Param("accountNumber") String accountNumber,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber) AND t.status = :status")
    Long countByAccountNumberAndStatus(@Param("accountNumber") String accountNumber, 
                                      @Param("status") TransactionStatus status);

    List<Transaction> findByStatusAndTransactionDateBefore(TransactionStatus status, LocalDateTime date);

    Boolean existsByTransactionId(String transactionId);
}
