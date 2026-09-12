package com.bank.controller;

import com.bank.dto.TransactionReport;
import com.bank.entity.User;
import com.bank.repository.UserRepository;
import com.bank.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserRepository userRepository;

    // public StatisticsController(StatisticsService statisticsService) {
    //     this.statisticsService = statisticsService;
    // }

    public StatisticsController(StatisticsService statisticsService, UserRepository userRepository) {
        this.statisticsService = statisticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/transactions/report")
    public TransactionReport transactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long ownerId = isAdmin() ? null : currentCustomerId().orElse(-1L);
        return statisticsService.getTransactionReport(from, to, ownerId);
    }

    @GetMapping("/accounts/count")
    public long countAccounts() { return statisticsService.countAccounts(); }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<Long> currentCustomerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getCustomer).filter(c -> c != null).map(c -> c.getId());
    }

    @GetMapping("/transactions/count")
    public long countTransactions() { return statisticsService.countTransactions(); }

    @GetMapping("/accounts/by-balance")
    public Map<String, Long> accountsByBalance() { return statisticsService.countAccountsByBalanceCategory(); }

    @GetMapping("/customers/by-location")
    public Map<String, Long> customersByLocation() { return statisticsService.countCustomersByLocation(); }
}