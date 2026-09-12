package com.bank.service;

import com.bank.entity.Account;
import com.bank.entity.AccountStatus;
import com.bank.entity.AccountStatusHistory;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountStatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository statusHistoryRepository;

    // public AccountService(AccountRepository accountRepository) {
    //     this.accountRepository = accountRepository;
    // }

    public AccountService(AccountRepository accountRepository,
                            AccountStatusHistoryRepository statusHistoryRepository) {
            this.accountRepository = accountRepository;
            this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#accountId")
    })
    public Account changeStatus(Long accountId, AccountStatus newStatus, String reason) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        if (newStatus == null) {
            throw new BusinessException("New status is required", HttpStatus.BAD_REQUEST);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessException("Cannot change status of a closed account", HttpStatus.BAD_REQUEST);
        }
        if (account.getStatus() == newStatus) {
            throw new BusinessException("Account is already in status " + newStatus, HttpStatus.BAD_REQUEST);
        }

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(newStatus);
        accountRepository.save(account);

        AccountStatusHistory history = new AccountStatusHistory(account, oldStatus, newStatus, reason);
        statusHistoryRepository.save(history);

        return account;
    }

    public List<AccountStatusHistory> getStatusHistory(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new BusinessException("Account not found", HttpStatus.NOT_FOUND);
        }
        return statusHistoryRepository.findByAccountIdOrderByChangedAtDesc(accountId);
    }

    @Cacheable("accounts")
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Cacheable(value = "accountById", key = "#id")
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<Account> findByCustomerId(Long customerId) {
        return accountRepository.findByOwnerId(customerId);
    }

    @CacheEvict(value = "accounts", allEntries = true)
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#id")
    })
    public Account updateAccount(Long id, Account account) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));
        existing.setBalance(account.getBalance());
        return accountRepository.save(existing);
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#id")
    })
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new BusinessException("Account not found", HttpStatus.NOT_FOUND);
        }
        accountRepository.deleteById(id);
    }
}