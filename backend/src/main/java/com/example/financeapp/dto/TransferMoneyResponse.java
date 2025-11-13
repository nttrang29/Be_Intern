package com.example.financeapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho response sau khi chuyển tiền thành công
 * Chứa thông tin về cả 2 transactions (expense và income)
 */
public class TransferMoneyResponse {

    // === GENERAL INFO ===
    private BigDecimal amount;
    private String currencyCode;
    private LocalDateTime transferredAt;
    private String note;

    // === FROM WALLET (SOURCE) INFO ===
    private Long fromWalletId;
    private String fromWalletName;
    private BigDecimal fromWalletBalanceBefore;
    private BigDecimal fromWalletBalanceAfter;
    private Long expenseTransactionId; // ID của transaction chi tiêu

    // === TO WALLET (DESTINATION) INFO ===
    private Long toWalletId;
    private String toWalletName;
    private BigDecimal toWalletBalanceBefore;
    private BigDecimal toWalletBalanceAfter;
    private Long incomeTransactionId; // ID của transaction thu nhập

    // === ADDITIONAL INFO ===
    private Long defaultViewWalletId; // ID của ví mặc định để xem chi tiết (ví nguồn)
    private Boolean toWalletIsShared; // Ví đích có phải là ví nhóm không
    private Integer toWalletMemberCount; // Số thành viên của ví đích
    private Boolean fromWalletIsShared; // Ví nguồn có phải là ví nhóm không
    private Integer fromWalletMemberCount; // Số thành viên của ví nguồn

    // Constructors
    public TransferMoneyResponse() {
    }

    // Getters & Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(LocalDateTime transferredAt) {
        this.transferredAt = transferredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getFromWalletId() {
        return fromWalletId;
    }

    public void setFromWalletId(Long fromWalletId) {
        this.fromWalletId = fromWalletId;
    }

    public String getFromWalletName() {
        return fromWalletName;
    }

    public void setFromWalletName(String fromWalletName) {
        this.fromWalletName = fromWalletName;
    }

    public BigDecimal getFromWalletBalanceBefore() {
        return fromWalletBalanceBefore;
    }

    public void setFromWalletBalanceBefore(BigDecimal fromWalletBalanceBefore) {
        this.fromWalletBalanceBefore = fromWalletBalanceBefore;
    }

    public BigDecimal getFromWalletBalanceAfter() {
        return fromWalletBalanceAfter;
    }

    public void setFromWalletBalanceAfter(BigDecimal fromWalletBalanceAfter) {
        this.fromWalletBalanceAfter = fromWalletBalanceAfter;
    }

    public Long getExpenseTransactionId() {
        return expenseTransactionId;
    }

    public void setExpenseTransactionId(Long expenseTransactionId) {
        this.expenseTransactionId = expenseTransactionId;
    }

    public Long getToWalletId() {
        return toWalletId;
    }

    public void setToWalletId(Long toWalletId) {
        this.toWalletId = toWalletId;
    }

    public String getToWalletName() {
        return toWalletName;
    }

    public void setToWalletName(String toWalletName) {
        this.toWalletName = toWalletName;
    }

    public BigDecimal getToWalletBalanceBefore() {
        return toWalletBalanceBefore;
    }

    public void setToWalletBalanceBefore(BigDecimal toWalletBalanceBefore) {
        this.toWalletBalanceBefore = toWalletBalanceBefore;
    }

    public BigDecimal getToWalletBalanceAfter() {
        return toWalletBalanceAfter;
    }

    public void setToWalletBalanceAfter(BigDecimal toWalletBalanceAfter) {
        this.toWalletBalanceAfter = toWalletBalanceAfter;
    }

    public Long getIncomeTransactionId() {
        return incomeTransactionId;
    }

    public void setIncomeTransactionId(Long incomeTransactionId) {
        this.incomeTransactionId = incomeTransactionId;
    }

    public Long getDefaultViewWalletId() {
        return defaultViewWalletId;
    }

    public void setDefaultViewWalletId(Long defaultViewWalletId) {
        this.defaultViewWalletId = defaultViewWalletId;
    }

    public Boolean getToWalletIsShared() {
        return toWalletIsShared;
    }

    public void setToWalletIsShared(Boolean toWalletIsShared) {
        this.toWalletIsShared = toWalletIsShared;
    }

    public Integer getToWalletMemberCount() {
        return toWalletMemberCount;
    }

    public void setToWalletMemberCount(Integer toWalletMemberCount) {
        this.toWalletMemberCount = toWalletMemberCount;
    }

    public Boolean getFromWalletIsShared() {
        return fromWalletIsShared;
    }

    public void setFromWalletIsShared(Boolean fromWalletIsShared) {
        this.fromWalletIsShared = fromWalletIsShared;
    }

    public Integer getFromWalletMemberCount() {
        return fromWalletMemberCount;
    }

    public void setFromWalletMemberCount(Integer fromWalletMemberCount) {
        this.fromWalletMemberCount = fromWalletMemberCount;
    }
}

