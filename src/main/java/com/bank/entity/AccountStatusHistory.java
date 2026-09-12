package com.bank.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_status_history")
public class AccountStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    private AccountStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private AccountStatus newStatus;

    @Column
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public AccountStatusHistory() {}

    public AccountStatusHistory(Account account, AccountStatus oldStatus,
                                 AccountStatus newStatus, String reason) {
        this.account = account;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public AccountStatus getOldStatus() { return oldStatus; }
    public AccountStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public LocalDateTime getChangedAt() { return changedAt; }
}