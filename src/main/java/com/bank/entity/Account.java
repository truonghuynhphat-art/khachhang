package com.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer owner;

    @Column(name = "transaction_limit", nullable = false)
    private BigDecimal transactionLimit;

    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    public Account() {}

    public Account(String accountNumber, BigDecimal balance, Customer owner,
                    BigDecimal transactionLimit, LocalDate openedDate) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.owner = owner;
        this.transactionLimit = transactionLimit;
        this.openedDate = openedDate;
        this.status = AccountStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Customer getOwner() { return owner; }
    public BigDecimal getTransactionLimit() { return transactionLimit; }
    public LocalDate getOpenedDate() { return openedDate; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
}