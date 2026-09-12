package com.bank.controller;

import com.bank.dto.AccountStatusChangeRequest;
import com.bank.entity.Account;
import com.bank.entity.AccountStatusHistory;
import com.bank.entity.User;
import com.bank.repository.UserRepository;
import com.bank.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.bank.entity.Transaction;
import com.bank.service.TransactionService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    // public AccountController(AccountService accountService, UserRepository userRepository) {
    //     this.accountService = accountService;
    //     this.userRepository = userRepository;
    // }

    public AccountController(AccountService accountService, UserRepository userRepository,
                              TransactionService transactionService) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable Long id) {
        Optional<Account> account = accountService.getAccountById(id);
        if (account.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin() && !ownsAccount(account.get())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(transactionService.getTransactionHistory(id));
    }

    @GetMapping
    public List<Account> getAll() {
        if (isAdmin()) {
            return accountService.getAllAccounts();
        }
        return currentCustomerId()
                .map(accountService::findByCustomerId)
                .orElse(List.of());
    }

    @GetMapping("/search")
    public List<Account> search(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) Long customerId) {

        if (!isAdmin()) {
            // Customer chỉ được search trong phạm vi chính mình, bỏ qua customerId họ tự truyền vào
            Long ownId = currentCustomerId().orElse(-1L);
            if (accountNumber != null) {
                return accountService.findByAccountNumber(accountNumber)
                        .filter(acc -> acc.getOwner().getId().equals(ownId))
                        .map(List::of).orElse(List.of());
            }
            return accountService.findByCustomerId(ownId);
        }

        if (accountNumber != null) {
            return accountService.findByAccountNumber(accountNumber)
                    .map(List::of).orElse(List.of());
        }
        if (customerId != null) {
            return accountService.findByCustomerId(customerId);
        }
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getById(@PathVariable Long id) {
        Optional<Account> account = accountService.getAccountById(id);
        if (account.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin() && !ownsAccount(account.get())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(account.get());
    }

    @PostMapping
    public Account create(@RequestBody Account account) { return accountService.createAccount(account); }

    @PutMapping("/{id}")
    public Account update(@PathVariable Long id, @RequestBody Account account) {
        return accountService.updateAccount(id, account);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { accountService.deleteAccount(id); }

    @PatchMapping("/{id}/status")
    public Account changeStatus(@PathVariable Long id,
                                 @RequestBody AccountStatusChangeRequest request) {
        return accountService.changeStatus(id, request.getStatus(), request.getReason());
    }

    @GetMapping("/{id}/status-history")
    public List<AccountStatusHistory> statusHistory(@PathVariable Long id) {
        return accountService.getStatusHistory(id);
    }

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

    private boolean ownsAccount(Account account) {
        return currentCustomerId()
                .map(id -> account.getOwner().getId().equals(id))
                .orElse(false);
    }
}