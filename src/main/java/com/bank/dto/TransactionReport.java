package com.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionReport {
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private long totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
    private BigDecimal maxAmount;
    private BigDecimal minAmount;

    public TransactionReport(LocalDateTime fromDate, LocalDateTime toDate, long totalTransactions,
                              BigDecimal totalAmount, BigDecimal averageAmount,
                              BigDecimal maxAmount, BigDecimal minAmount) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
        this.maxAmount = maxAmount;
        this.minAmount = minAmount;
    }

    public LocalDateTime getFromDate() { return fromDate; }
    public LocalDateTime getToDate() { return toDate; }
    public long getTotalTransactions() { return totalTransactions; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getAverageAmount() { return averageAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public BigDecimal getMinAmount() { return minAmount; }
}