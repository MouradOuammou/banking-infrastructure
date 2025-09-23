package com.banking.accounts.controller;

import com.banking.accounts.dto.*;

import com.banking.accounts.entity.*;
import com.banking.accounts.service.*;
import io.micrometer.core.annotation.*;
import jakarta.validation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.awt.print.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "accounts.create", description = "Time taken to create account")
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDto account = accountService.createAccount(request);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "accounts.get", description = "Time taken to get account")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{accountNumber}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "accounts.getByNumber", description = "Time taken to get account by number")
    public ResponseEntity<AccountDto> getAccountByNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "accounts.getByUser", description = "Time taken to get accounts by user")
    public ResponseEntity<List<AccountDto>> getAccountsByUserId(@PathVariable Long userId) {
        List<AccountDto> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/user/{userId}/paged")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<AccountDto>> getAccountsByUserIdPaged(
            @PathVariable Long userId, 
            Pageable pageable) {
        Page<AccountDto> accounts = accountService.getAccountsByUserId(userId, (org.springframework.data.domain.Pageable) pageable);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountDto>> getActiveAccountsByUserId(@PathVariable Long userId) {
        List<AccountDto> accounts = accountService.getActiveAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}/balance")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "accounts.updateBalance", description = "Time taken to update balance")
    public ResponseEntity<AccountDto> updateBalance(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateBalanceRequest request) {
        AccountDto account = accountService.updateBalance(id, request);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "accounts.updateStatus", description = "Time taken to update status")
    public ResponseEntity<AccountDto> updateAccountStatus(
            @PathVariable Long id, 
            @RequestParam AccountStatus status) {
        AccountDto account = accountService.updateAccountStatus(id, status);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/user/{userId}/count")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Long> countActiveAccountsByUserId(@PathVariable Long userId) {
        Long count = accountService.countActiveAccountsByUserId(userId);
        return ResponseEntity.ok(count);
    }
}
