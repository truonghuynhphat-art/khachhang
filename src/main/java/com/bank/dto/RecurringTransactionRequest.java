package com.bank.dto;

import com.bank.entity.Frequency;

import java.math.BigDecimal;

public class RecurringTransactionRequest {
    private Long accountId;
    private String type;       // DEPOSIT hoặc WITHDRAW
    private BigDecimal amount;
    private String location;
    private Frequency frequency;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
}