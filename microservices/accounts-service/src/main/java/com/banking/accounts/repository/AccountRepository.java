package com.banking.accounts.repository;

import com.banking.accounts.entity.Account;
import com.banking.accounts.entity.AccountStatus;
import com.banking.accounts.entity.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    List<Account> findByUserIdAndStatus(Long userId, AccountStatus status);

    List<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);

    Page<Account> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.userId = :userId AND a.status = :status")
    Page<Account> findByUserIdAndStatus(@Param("userId") Long userId,
                                       @Param("status") AccountStatus status, 
                                       Pageable pageable);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.userId = :userId AND a.status = 'ACTIVE'")
    Long countActiveAccountsByUserId(@Param("userId") Long userId);

    Boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.iban = :iban")
    Optional<Account> findByIban(@Param("iban") String iban);
}
