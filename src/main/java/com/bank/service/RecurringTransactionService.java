package com.bank.service;

import com.bank.entity.*;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.RecurringTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public RecurringTransactionService(RecurringTransactionRepository recurringRepository,
                                        AccountRepository accountRepository,
                                        TransactionService transactionService) {
        this.recurringRepository = recurringRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    public RecurringTransaction create(Long accountId, String type, BigDecimal amount,
                                        String location, Frequency frequency, Long requesterCustomerId,
                                        boolean isAdmin) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        if (!isAdmin && !account.getOwner().getId().equals(requesterCustomerId)) {
            throw new BusinessException("You can only create recurring transactions for your own account",
                    HttpStatus.FORBIDDEN);
        }
        if (!"DEPOSIT".equals(type) && !"WITHDRAW".equals(type)) {
            throw new BusinessException("Type must be DEPOSIT or WITHDRAW", HttpStatus.BAD_REQUEST);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime nextRun = computeNextRun(LocalDateTime.now(), frequency);
        RecurringTransaction rt = new RecurringTransaction(account, type, amount, location, frequency, nextRun);
        return recurringRepository.save(rt);
    }

    public List<RecurringTransaction> findMine(Long customerId, boolean isAdmin) {
        if (isAdmin) {
            return recurringRepository.findAll();
        }
        return recurringRepository.findByAccountOwnerId(customerId);
    }

    public void cancel(Long id, Long requesterCustomerId, boolean isAdmin) {
        RecurringTransaction rt = recurringRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Recurring transaction not found", HttpStatus.NOT_FOUND));

        if (!isAdmin && !rt.getAccount().getOwner().getId().equals(requesterCustomerId)) {
            throw new BusinessException("You can only cancel your own recurring transactions", HttpStatus.FORBIDDEN);
        }
        rt.setActive(false);
        recurringRepository.save(rt);
    }

    /** Được gọi bởi cron job - xử lý mọi recurring transaction đã đến hạn. */
    public void processDueTransactions() {
        List<RecurringTransaction> due = recurringRepository
                .findByActiveTrueAndNextRunDateLessThanEqual(LocalDateTime.now());

        for (RecurringTransaction rt : due) {
            try {
                if ("DEPOSIT".equals(rt.getType())) {
                    transactionService.deposit(rt.getAccount().getId(), rt.getAmount(), rt.getLocation());
                } else {
                    transactionService.withdraw(rt.getAccount().getId(), rt.getAmount(), rt.getLocation());
                }
                rt.setNextRunDate(computeNextRun(rt.getNextRunDate(), rt.getFrequency()));
            } catch (BusinessException e) {
                // Ví dụ: withdraw định kỳ vượt hạn mức hoặc không đủ số dư tại thời điểm chạy.
                // Không dừng cả batch vì 1 giao dịch lỗi - bỏ qua lần này, vẫn dời sang kỳ tiếp theo,
                // tránh việc 1 tài khoản lỗi kẹt mãi ở "quá hạn" và bị cron thử lại vô hạn mỗi lần chạy.
                rt.setNextRunDate(computeNextRun(rt.getNextRunDate(), rt.getFrequency()));
            }
            recurringRepository.save(rt);
        }
    }

    private LocalDateTime computeNextRun(LocalDateTime from, Frequency frequency) {
        return switch (frequency) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
        };
    }
}