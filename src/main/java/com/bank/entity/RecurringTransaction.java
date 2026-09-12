package com.bank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_transaction")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String type; // DEPOSIT hoặc WITHDRAW, tái dùng cùng convention với Transaction.type

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "next_run_date", nullable = false)
    private LocalDateTime nextRunDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecurringTransaction() {}

    public RecurringTransaction(Account account, String type, BigDecimal amount, String location,
                                 Frequency frequency, LocalDateTime nextRunDate) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.location = location;
        this.frequency = frequency;
        this.nextRunDate = nextRunDate;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getLocation() { return location; }
    public Frequency getFrequency() { return frequency; }
    public LocalDateTime getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDateTime nextRunDate) { this.nextRunDate = nextRunDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}