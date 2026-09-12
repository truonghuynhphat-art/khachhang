package com.bank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_type")
public class CustomerType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CustomerTypeName name;

    @Column(name = "max_transaction_limit", nullable = false)
    private BigDecimal maxTransactionLimit;

    public CustomerType() {}

    public CustomerType(CustomerTypeName name, BigDecimal maxTransactionLimit) {
        this.name = name;
        this.maxTransactionLimit = maxTransactionLimit;
    }

    public Long getId() { return id; }
    public CustomerTypeName getName() { return name; }
    public BigDecimal getMaxTransactionLimit() { return maxTransactionLimit; }
    public void setMaxTransactionLimit(BigDecimal maxTransactionLimit) { this.maxTransactionLimit = maxTransactionLimit; }
}