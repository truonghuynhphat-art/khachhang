package com.bank.service;

import com.bank.entity.Account;
import com.bank.entity.Customer;
import com.bank.dto.TransactionReport;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final BigDecimal LOW_THRESHOLD = new BigDecimal("10000000");
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("100000000");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    public StatisticsService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    public TransactionReport getTransactionReport(LocalDateTime from, LocalDateTime to, Long ownerId) {
        if (from == null || to == null) {
            throw new BusinessException("fromDate and toDate are required", HttpStatus.BAD_REQUEST);
        }
        if (from.isAfter(to)) {
            throw new BusinessException("fromDate must be before toDate", HttpStatus.BAD_REQUEST);
        }

        // Object[] row = transactionRepository.aggregateStats(from, to, ownerId);
        Object[] row = transactionRepository.aggregateStats(from, to, ownerId).get(0);

        long count = ((Number) row[0]).longValue();
        BigDecimal total = toBigDecimal(row[1]);
        BigDecimal average = toBigDecimal(row[2]);
        BigDecimal max = toBigDecimal(row[3]);
        BigDecimal min = toBigDecimal(row[4]);

        return new TransactionReport(from, to, count, total, average, max, min);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    public long countAccounts() { return accountRepository.count(); }

    public long countTransactions() { return transactionRepository.count(); }

    public Map<String, Long> countAccountsByBalanceCategory() {
        List<Account> accounts = accountRepository.findAll();

        long low = accounts.stream().filter(a -> a.getBalance().compareTo(LOW_THRESHOLD) < 0).count();
        long medium = accounts.stream()
                .filter(a -> a.getBalance().compareTo(LOW_THRESHOLD) >= 0
                        && a.getBalance().compareTo(HIGH_THRESHOLD) < 0)
                .count();
        long high = accounts.stream().filter(a -> a.getBalance().compareTo(HIGH_THRESHOLD) >= 0).count();

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("LOW", low);
        result.put("MEDIUM", medium);
        result.put("HIGH", high);
        return result;
    }

    public Map<String, Long> countCustomersByLocation() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getLocation() == null ? "UNKNOWN" : c.getLocation(),
                        Collectors.counting()));
    }
}