package com.bank.repository;

import com.bank.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {
    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDesc(Long accountId);
}