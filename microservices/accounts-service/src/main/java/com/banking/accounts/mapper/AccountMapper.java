package com.banking.accounts.mapper;

import com.banking.accounts.dto.AccountDto;
import com.banking.accounts.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDto toDto(Account account) {
        if (account == null) {
            return null;
        }

        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setUserId(account.getUserId());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setAvailableBalance(account.getAvailableBalance());
        dto.setOverdraftLimit(account.getOverdraftLimit());
        dto.setCurrency(account.getCurrency());
        dto.setStatus(account.getStatus());
        dto.setBranchCode(account.getBranchCode());
        dto.setIban(account.getIban());
        dto.setBic(account.getBic());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());

        return dto;
    }

    public Account toEntity(AccountDto dto) {
        if (dto == null) {
            return null;
        }

        Account account = new Account();
        account.setId(dto.getId());
        account.setAccountNumber(dto.getAccountNumber());
        account.setUserId(dto.getUserId());
        account.setAccountType(dto.getAccountType());
        account.setBalance(dto.getBalance());
        account.setAvailableBalance(dto.getAvailableBalance());
        account.setOverdraftLimit(dto.getOverdraftLimit());
        account.setCurrency(dto.getCurrency());
        account.setStatus(dto.getStatus());
        account.setBranchCode(dto.getBranchCode());
        account.setIban(dto.getIban());
        account.setBic(dto.getBic());

        return account;
    }
}
