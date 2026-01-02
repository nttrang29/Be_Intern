package com.example.financeapp.fund.dto;

import com.example.financeapp.fund.entity.FundTransaction;
import com.example.financeapp.fund.entity.FundTransactionStatus;
import com.example.financeapp.fund.entity.FundTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FundTransactionResponse {
    private Long id;
    private FundTransactionType type;
    private FundTransactionStatus status;
    private BigDecimal amount;
    private String message;
    private LocalDateTime createdAt;

    public static FundTransactionResponse from(FundTransaction tx) {
        FundTransactionResponse res = new FundTransactionResponse();
        res.setId(tx.getId());
        res.setType(tx.getType());
        res.setStatus(tx.getStatus());
        res.setAmount(tx.getAmount());
        res.setMessage(tx.getMessage());
        res.setCreatedAt(tx.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FundTransactionType getType() {
        return type;
    }

    public void setType(FundTransactionType type) {
        this.type = type;
    }

    public FundTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(FundTransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

