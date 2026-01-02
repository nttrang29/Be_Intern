package com.example.financeapp.wallet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử gộp ví
 * Dùng cho audit trail và cho phép user xem lại lịch sử
 */
@Entity
@Table(
        name = "wallet_merge_history",
        indexes = {
                @Index(name = "idx_user_merged_at", columnList = "user_id,merged_at"),
                @Index(name = "idx_target_wallet", columnList = "target_wallet_id"),
                @Index(name = "idx_source_wallet", columnList = "source_wallet_id"),
                @Index(name = "idx_merged_at", columnList = "merged_at")
        }
)
public class WalletMergeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merge_id")
    private Long mergeId;

    // ===== USER INFO =====
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ===== SOURCE WALLET INFO (ví bị xóa) =====
    @Column(name = "source_wallet_id", nullable = false)
    private Long sourceWalletId;

    @Column(name = "source_wallet_name", length = 100, nullable = false)
    private String sourceWalletName;

    @Column(name = "source_currency", length = 3, nullable = false)
    private String sourceCurrency;

    @Column(name = "source_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal sourceBalance;

    @Column(name = "source_transaction_count", nullable = false)
    private Integer sourceTransactionCount = 0;

    // ===== TARGET WALLET INFO (ví giữ lại) =====
    @Column(name = "target_wallet_id", nullable = false)
    private Long targetWalletId;

    @Column(name = "target_wallet_name", length = 100, nullable = false)
    private String targetWalletName;

    @Column(name = "target_currency", length = 3, nullable = false)
    private String targetCurrency;

    @Column(name = "target_balance_before", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetBalanceBefore;

    @Column(name = "target_balance_after", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetBalanceAfter;

    @Column(name = "target_transaction_count_before", nullable = false)
    private Integer targetTransactionCountBefore = 0;

    // ===== METADATA =====
    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt = LocalDateTime.now();

    @Column(name = "merge_duration_ms")
    private Long mergeDurationMs;

    // ===== CONSTRUCTORS =====
    public WalletMergeHistory() {}

    // ===== GETTERS & SETTERS =====
    public Long getMergeId() {
        return mergeId;
    }

    public void setMergeId(Long mergeId) {
        this.mergeId = mergeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSourceWalletId() {
        return sourceWalletId;
    }

    public void setSourceWalletId(Long sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }

    public String getSourceWalletName() {
        return sourceWalletName;
    }

    public void setSourceWalletName(String sourceWalletName) {
        this.sourceWalletName = sourceWalletName;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public BigDecimal getSourceBalance() {
        return sourceBalance;
    }

    public void setSourceBalance(BigDecimal sourceBalance) {
        this.sourceBalance = sourceBalance;
    }

    public Integer getSourceTransactionCount() {
        return sourceTransactionCount;
    }

    public void setSourceTransactionCount(Integer sourceTransactionCount) {
        this.sourceTransactionCount = sourceTransactionCount;
    }

    public Long getTargetWalletId() {
        return targetWalletId;
    }

    public void setTargetWalletId(Long targetWalletId) {
        this.targetWalletId = targetWalletId;
    }

    public String getTargetWalletName() {
        return targetWalletName;
    }

    public void setTargetWalletName(String targetWalletName) {
        this.targetWalletName = targetWalletName;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getTargetBalanceBefore() {
        return targetBalanceBefore;
    }

    public void setTargetBalanceBefore(BigDecimal targetBalanceBefore) {
        this.targetBalanceBefore = targetBalanceBefore;
    }

    public BigDecimal getTargetBalanceAfter() {
        return targetBalanceAfter;
    }

    public void setTargetBalanceAfter(BigDecimal targetBalanceAfter) {
        this.targetBalanceAfter = targetBalanceAfter;
    }

    public Integer getTargetTransactionCountBefore() {
        return targetTransactionCountBefore;
    }

    public void setTargetTransactionCountBefore(Integer targetTransactionCountBefore) {
        this.targetTransactionCountBefore = targetTransactionCountBefore;
    }

    public LocalDateTime getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(LocalDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }

    public Long getMergeDurationMs() {
        return mergeDurationMs;
    }

    public void setMergeDurationMs(Long mergeDurationMs) {
        this.mergeDurationMs = mergeDurationMs;
    }
}

