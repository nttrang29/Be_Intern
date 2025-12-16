package com.example.financeapp.scheduledtransaction.dto;

import com.example.financeapp.scheduledtransaction.entity.ScheduledTransactionLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO response cho execution log
 */
public class ScheduledTransactionLogResponse {
    private Long logId;
    private Long scheduleId;
    private String status;
    private String message;
    private BigDecimal amount;
    private BigDecimal walletBalanceBefore;
    private BigDecimal walletBalanceAfter;
    private LocalDateTime executionTime;
    private LocalDateTime createdAt;

    public ScheduledTransactionLogResponse() {}

    public static ScheduledTransactionLogResponse fromEntity(ScheduledTransactionLog log) {
        ScheduledTransactionLogResponse response = new ScheduledTransactionLogResponse();
        response.setLogId(log.getLogId());
        response.setScheduleId(log.getScheduledTransaction().getScheduleId());
        response.setStatus(log.getStatus().name());
        response.setMessage(log.getMessage());
        response.setAmount(log.getAmount());
        response.setWalletBalanceBefore(log.getWalletBalanceBefore());
        response.setWalletBalanceAfter(log.getWalletBalanceAfter());
        response.setExecutionTime(log.getExecutionTime());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    // Getters & Setters
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getWalletBalanceBefore() { return walletBalanceBefore; }
    public void setWalletBalanceBefore(BigDecimal walletBalanceBefore) { this.walletBalanceBefore = walletBalanceBefore; }

    public BigDecimal getWalletBalanceAfter() { return walletBalanceAfter; }
    public void setWalletBalanceAfter(BigDecimal walletBalanceAfter) { this.walletBalanceAfter = walletBalanceAfter; }

    public LocalDateTime getExecutionTime() { return executionTime; }
    public void setExecutionTime(LocalDateTime executionTime) { this.executionTime = executionTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
