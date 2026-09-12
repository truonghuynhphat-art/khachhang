package com.bank.controller;

import com.bank.dto.RecurringTransactionRequest;
import com.bank.entity.RecurringTransaction;
import com.bank.entity.User;
import com.bank.repository.UserRepository;
import com.bank.service.RecurringTransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;
    private final UserRepository userRepository;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService,
                                           UserRepository userRepository) {
        this.recurringTransactionService = recurringTransactionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public RecurringTransaction create(@RequestBody RecurringTransactionRequest req) {
        return recurringTransactionService.create(
                req.getAccountId(), req.getType(), req.getAmount(), req.getLocation(), req.getFrequency(),
                currentCustomerId().orElse(-1L), isAdmin());
    }

    @GetMapping
    public List<RecurringTransaction> getAll() {
        return recurringTransactionService.findMine(currentCustomerId().orElse(-1L), isAdmin());
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id) {
        recurringTransactionService.cancel(id, currentCustomerId().orElse(-1L), isAdmin());
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<Long> currentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getCustomer).filter(c -> c != null).map(c -> c.getId());
    }
}