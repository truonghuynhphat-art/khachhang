package com.bank.repository;

import com.bank.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByActiveTrueAndNextRunDateLessThanEqual(LocalDateTime now);
    List<RecurringTransaction> findByAccountOwnerId(Long customerId);
}