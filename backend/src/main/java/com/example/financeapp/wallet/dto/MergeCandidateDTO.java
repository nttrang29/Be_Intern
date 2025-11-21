package com.example.financeapp.dto;

import java.math.BigDecimal;

/**
 * DTO cho ví có thể gộp (candidate wallet)
 */
public class MergeCandidateDTO {

    private Long walletId;
    private String walletName;
    private String currencyCode;
    private BigDecimal balance;
    private int transactionCount;
    private boolean isDefault;
    private boolean canMerge;
    private String reason; // Lý do nếu không thể merge

    // Constructors
    public MergeCandidateDTO() {}

    public MergeCandidateDTO(Long walletId, String walletName, String currencyCode,
                             BigDecimal balance, int transactionCount, boolean isDefault) {
        this.walletId = walletId;
        this.walletName = walletName;
        this.currencyCode = currencyCode;
        this.balance = balance;
        this.transactionCount = transactionCount;
        this.isDefault = isDefault;
        this.canMerge = true;
        this.reason = null;
    }

    // Getters & Setters
    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isCanMerge() {
        return canMerge;
    }

    public void setCanMerge(boolean canMerge) {
        this.canMerge = canMerge;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

