package com.example.financeapp.dto;

import java.math.BigDecimal;

/**
 * DTO cho response sau khi xóa ví
 * Chứa thông tin về ví đã xóa và các hành động liên quan
 */
public class DeleteWalletResponse {

    private Long deletedWalletId;
    private String deletedWalletName;
    private BigDecimal balance;
    private String currencyCode;
    private Integer transactionsDeleted;
    private Integer membersRemoved;
    private Boolean wasDefault;
    private Long newDefaultWalletId;
    private String newDefaultWalletName;

    // Constructors
    public DeleteWalletResponse() {
    }

    // Getters & Setters
    public Long getDeletedWalletId() {
        return deletedWalletId;
    }

    public void setDeletedWalletId(Long deletedWalletId) {
        this.deletedWalletId = deletedWalletId;
    }

    public String getDeletedWalletName() {
        return deletedWalletName;
    }

    public void setDeletedWalletName(String deletedWalletName) {
        this.deletedWalletName = deletedWalletName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getTransactionsDeleted() {
        return transactionsDeleted;
    }

    public void setTransactionsDeleted(Integer transactionsDeleted) {
        this.transactionsDeleted = transactionsDeleted;
    }

    public Integer getMembersRemoved() {
        return membersRemoved;
    }

    public void setMembersRemoved(Integer membersRemoved) {
        this.membersRemoved = membersRemoved;
    }

    public Boolean getWasDefault() {
        return wasDefault;
    }

    public void setWasDefault(Boolean wasDefault) {
        this.wasDefault = wasDefault;
    }

    public Long getNewDefaultWalletId() {
        return newDefaultWalletId;
    }

    public void setNewDefaultWalletId(Long newDefaultWalletId) {
        this.newDefaultWalletId = newDefaultWalletId;
    }

    public String getNewDefaultWalletName() {
        return newDefaultWalletName;
    }

    public void setNewDefaultWalletName(String newDefaultWalletName) {
        this.newDefaultWalletName = newDefaultWalletName;
    }
}

