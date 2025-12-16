package com.example.financeapp.scheduledtransaction.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử thực hiện của scheduled transaction
 */
@Entity
@Table(name = "scheduled_transaction_logs")
public class ScheduledTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ScheduledTransaction scheduledTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LogStatus status;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "amount", precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(name = "wallet_balance_before", precision = 20, scale = 8)
    private BigDecimal walletBalanceBefore;

    @Column(name = "wallet_balance_after", precision = 20, scale = 8)
    private BigDecimal walletBalanceAfter;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum LogStatus {
        COMPLETED,  // Thực hiện thành công
        FAILED,     // Thực hiện thất bại
        SKIPPED     // Bỏ qua (ví đã bị xóa, etc.)
    }

    // Getters & Setters
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public ScheduledTransaction getScheduledTransaction() { return scheduledTransaction; }
    public void setScheduledTransaction(ScheduledTransaction scheduledTransaction) { this.scheduledTransaction = scheduledTransaction; }

    public LogStatus getStatus() { return status; }
    public void setStatus(LogStatus status) { this.status = status; }

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
