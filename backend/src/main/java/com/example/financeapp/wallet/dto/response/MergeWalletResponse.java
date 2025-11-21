package com.example.financeapp.wallet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho response sau khi gộp ví thành công
 */
public class MergeWalletResponse {

    private boolean success;
    private String message;
    private Long targetWalletId;
    private String targetWalletName;
    private BigDecimal finalBalance;
    private String finalCurrency;
    private int mergedTransactions;
    private String sourceWalletName;
    private boolean wasDefaultTransferred;
    private Long mergeHistoryId;
    private LocalDateTime mergedAt;

    // Constructors
    public MergeWalletResponse() {}

    // Getters & Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public BigDecimal getFinalBalance() {
        return finalBalance;
    }

    public void setFinalBalance(BigDecimal finalBalance) {
        this.finalBalance = finalBalance;
    }

    public String getFinalCurrency() {
        return finalCurrency;
    }

    public void setFinalCurrency(String finalCurrency) {
        this.finalCurrency = finalCurrency;
    }

    public int getMergedTransactions() {
        return mergedTransactions;
    }

    public void setMergedTransactions(int mergedTransactions) {
        this.mergedTransactions = mergedTransactions;
    }

    public String getSourceWalletName() {
        return sourceWalletName;
    }

    public void setSourceWalletName(String sourceWalletName) {
        this.sourceWalletName = sourceWalletName;
    }

    public boolean isWasDefaultTransferred() {
        return wasDefaultTransferred;
    }

    public void setWasDefaultTransferred(boolean wasDefaultTransferred) {
        this.wasDefaultTransferred = wasDefaultTransferred;
    }

    public Long getMergeHistoryId() {
        return mergeHistoryId;
    }

    public void setMergeHistoryId(Long mergeHistoryId) {
        this.mergeHistoryId = mergeHistoryId;
    }

    public LocalDateTime getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(LocalDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }
}

