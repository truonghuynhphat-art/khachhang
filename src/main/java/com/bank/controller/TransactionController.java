package com.bank.controller;

import com.bank.entity.Transaction;
import com.bank.entity.User;
import com.bank.entity.Account;
import com.bank.exception.BusinessException;
import com.bank.repository.UserRepository;
import com.bank.service.AccountService;
import com.bank.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final AccountService accountService;

    // public TransactionController(TransactionService transactionService, UserRepository userRepository) {
    //     this.transactionService = transactionService;
    //     this.userRepository = userRepository;
    // }

    public TransactionController(TransactionService transactionService, UserRepository userRepository,
                                  AccountService accountService) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.accountService = accountService;
    }

    @PostMapping("/withdraw")
    public Transaction withdraw(@RequestParam Long accountId, @RequestParam BigDecimal amount,
                                 @RequestParam String location) {
        checkOwnership(accountId);
        return transactionService.withdraw(accountId, amount, location);
    }

    @PostMapping("/deposit")
    public Transaction deposit(@RequestParam Long accountId, @RequestParam BigDecimal amount,
                                @RequestParam String location) {
        checkOwnership(accountId);
        return transactionService.deposit(accountId, amount, location);
    }

    @PostMapping("/transfer")
    public Transaction transfer(@RequestParam Long fromAccountId, @RequestParam Long toAccountId,
                                 @RequestParam BigDecimal amount, @RequestParam String location) {
        checkOwnership(fromAccountId);
        return transactionService.transfer(fromAccountId, toAccountId, amount, location);
    }

    private void checkOwnership(Long accountId) {
        if (isAdmin()) return;

        Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        Long ownId = currentCustomerId().orElse(-1L);
        if (!account.getOwner().getId().equals(ownId)) {
            throw new BusinessException("You can only perform transactions on your own account", HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/search")
    public Page<Transaction> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal amountFrom,
            @RequestParam(required = false) BigDecimal amountTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Long ownerFilter = isAdmin() ? null : currentCustomerId().orElse(-1L);
        return transactionService.search(type, amountFrom, amountTo, createdFrom, createdTo, ownerFilter, pageable);
    }

    @GetMapping
    public List<Transaction> getAll() {
        if (isAdmin()) {
            return transactionService.findAll();
        }
        Long ownId = currentCustomerId().orElse(-1L);
        return transactionService.findAll().stream()
                .filter(t -> t.getAccount().getOwner().getId().equals(ownId))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        Optional<Transaction> tx = transactionService.findById(id);
        if (tx.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin() && !ownsTransaction(tx.get())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(tx.get());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { transactionService.deleteById(id); }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<Long> currentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getCustomer)
                .filter(c -> c != null)
                .map(c -> c.getId());
    }

    private boolean ownsTransaction(Transaction tx) {
        return currentCustomerId()
                .map(id -> tx.getAccount().getOwner().getId().equals(id))
                .orElse(false);
    }
}