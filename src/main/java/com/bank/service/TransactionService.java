package com.bank.service;

import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.spec.TransactionSpecification;
import com.bank.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AlertService alertService;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               AlertService alertService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.alertService = alertService;
    }

    @Cacheable(value = "transactionHistory", key = "#accountId")
    public List<Transaction> getTransactionHistory(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#accountId"),
            @CacheEvict(value = "transactionHistory", key = "#accountId")
    })
    public Transaction withdraw(Long accountId, BigDecimal amount, String location) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        // if (amount.compareTo(account.getTransactionLimit()) > 0) {
        //     throw new BusinessException("Transaction exceeds transaction limit", HttpStatus.BAD_REQUEST);
        // }
        checkTransactionLimit(account, amount);
        if (amount.compareTo(account.getBalance()) > 0) {
            throw new BusinessException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction(account, "WITHDRAW", amount, fee, location);
        Transaction saved = transactionRepository.save(tx);
        alertService.checkAndFlag(saved);
        return saved;
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#accountId"),
            @CacheEvict(value = "transactionHistory", key = "#accountId")
    })
    public Transaction deposit(Long accountId, BigDecimal amount, String location) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction(account, "DEPOSIT", amount, fee, location);
        Transaction saved = transactionRepository.save(tx);
        alertService.checkAndFlag(saved);
        return saved;
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#fromAccountId"),
            @CacheEvict(value = "accountById", key = "#toAccountId"),
            @CacheEvict(value = "transactionHistory", key = "#fromAccountId"),
            @CacheEvict(value = "transactionHistory", key = "#toAccountId")
    })
    @Transactional
    public Transaction transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String location) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new BusinessException("Cannot transfer to the same account", HttpStatus.BAD_REQUEST);
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new BusinessException("Source account not found", HttpStatus.NOT_FOUND));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new BusinessException("Target account not found", HttpStatus.NOT_FOUND));

        // if (amount.compareTo(fromAccount.getTransactionLimit()) > 0) {
        //     throw new BusinessException("Transaction exceeds transaction limit", HttpStatus.BAD_REQUEST);
        // }
        checkTransactionLimit(fromAccount, amount);
        if (amount.compareTo(fromAccount.getBalance()) > 0) {
            throw new BusinessException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transferOut = new Transaction(fromAccount, "TRANSFER_OUT", amount, fee, location);
        Transaction transferIn = new Transaction(toAccount, "TRANSFER_IN", amount, BigDecimal.ZERO, location);
        Transaction savedIn = transactionRepository.save(transferIn);
        Transaction savedOut = transactionRepository.save(transferOut);
        alertService.checkAndFlag(savedIn);
        alertService.checkAndFlag(savedOut);
        return savedOut;
    }

    /**
     * Cập nhật các trường mô tả của một giao dịch đã ghi nhận (location, fee, type, amount).
     * Lưu ý: thao tác này KHÔNG tự động điều chỉnh lại số dư (balance) của tài khoản liên quan,
     * vì việc thay đổi số dư phải luôn đi qua withdraw/deposit/transfer để đảm bảo tính nhất quán
     * và tuân thủ kiểm tra hạn mức. API này chỉ dùng để sửa thông tin/điều chỉnh dữ liệu giao dịch
     * (ví dụ sửa địa điểm ghi nhận sai, sửa phí).
     */
    @Caching(evict = {
            @CacheEvict(value = "transactionHistory", allEntries = true),
            @CacheEvict(value = "accounts", allEntries = true)
    })
    public Transaction updateTransaction(Long id, Transaction updated) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction not found", HttpStatus.NOT_FOUND));

        if (updated.getType() != null) {
            existing.setType(updated.getType());
        }
        if (updated.getAmount() != null) {
            if (updated.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
            }
            existing.setAmount(updated.getAmount());
        }
        if (updated.getFee() != null) {
            existing.setFee(updated.getFee());
        }
        if (updated.getLocation() != null) {
            existing.setLocation(updated.getLocation());
        }
        return transactionRepository.save(existing);
    }

    public Page<Transaction> search(String type, BigDecimal amountFrom, BigDecimal amountTo,
                                     LocalDateTime createdFrom, LocalDateTime createdTo,
                                     Long ownerCustomerId, Pageable pageable) {
        Specification<Transaction> spec = Specification
                .where(TransactionSpecification.hasType(type))
                .and(TransactionSpecification.amountFrom(amountFrom))
                .and(TransactionSpecification.amountTo(amountTo))
                .and(TransactionSpecification.createdFrom(createdFrom))
                .and(TransactionSpecification.createdTo(createdTo))
                .and(ownerCustomerId == null ? null :
                        (root, query, cb) -> cb.equal(root.get("account").get("owner").get("id"), ownerCustomerId));
        return transactionRepository.findAll(spec, pageable);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Caching(evict = {
            @CacheEvict(value = "transactionHistory", allEntries = true),
            @CacheEvict(value = "accounts", allEntries = true)
    })
    public void deleteById(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new BusinessException("Transaction not found", HttpStatus.NOT_FOUND);
        }
        transactionRepository.deleteById(id);
    }

    private void checkTransactionLimit(Account account, BigDecimal amount) {
        BigDecimal accountLimit = account.getTransactionLimit();
        BigDecimal customerTypeLimit = account.getOwner().getCustomerType().getMaxTransactionLimit();
        BigDecimal effectiveLimit = accountLimit.min(customerTypeLimit);

        if (amount.compareTo(effectiveLimit) > 0) {
            throw new BusinessException(
                    "Transaction exceeds allowed limit (" + effectiveLimit + ")",
                    HttpStatus.BAD_REQUEST);
        }
    }
}