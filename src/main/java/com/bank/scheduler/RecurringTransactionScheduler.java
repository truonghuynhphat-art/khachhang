package com.bank.scheduler;

import com.bank.service.RecurringTransactionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionScheduler(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    // Chạy mỗi giờ đúng phút 0. Đổi cron expression tùy nhu cầu test (VD mỗi phút: "0 * * * * *")
    @Scheduled(cron = "0 0 * * * *")
    public void runDueRecurringTransactions() {
        recurringTransactionService.processDueTransactions();
    }
}