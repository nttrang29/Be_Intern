package com.example.financeapp.backup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu được backup (serialize ra JSON)
 */
public class BackupPayload {
    private BackupUser user;
    private List<BackupWallet> wallets;
    private List<BackupTransaction> transactions;
    private List<BackupBudget> budgets;
    private LocalDateTime generatedAt = LocalDateTime.now();

    public BackupUser getUser() {
        return user;
    }

    public void setUser(BackupUser user) {
        this.user = user;
    }

    public List<BackupWallet> getWallets() {
        return wallets;
    }

    public void setWallets(List<BackupWallet> wallets) {
        this.wallets = wallets;
    }

    public List<BackupTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<BackupTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<BackupBudget> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BackupBudget> budgets) {
        this.budgets = budgets;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public static class BackupUser {
        private Long userId;
        private String fullName;
        private String email;
        private LocalDateTime createdAt;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class BackupWallet {
        private Long walletId;
        private String walletName;
        private String currencyCode;
        private BigDecimal balance;
        private boolean isDefault;
        private String description;

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

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean aDefault) {
            isDefault = aDefault;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class BackupTransaction {
        private Long transactionId;
        private Long walletId;
        private String walletName;
        private String transactionType;
        private String categoryName;
        private BigDecimal amount;
        private LocalDateTime transactionDate;
        private String note;

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }

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

        public String getTransactionType() {
            return transactionType;
        }

        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public LocalDateTime getTransactionDate() {
            return transactionDate;
        }

        public void setTransactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class BackupBudget {
        private Long budgetId;
        private String categoryName;
        private String walletName;
        private BigDecimal amountLimit;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private String status;
        private String budgetStatus;

        public Long getBudgetId() {
            return budgetId;
        }

        public void setBudgetId(Long budgetId) {
            this.budgetId = budgetId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getWalletName() {
            return walletName;
        }

        public void setWalletName(String walletName) {
            this.walletName = walletName;
        }

        public BigDecimal getAmountLimit() {
            return amountLimit;
        }

        public void setAmountLimit(BigDecimal amountLimit) {
            this.amountLimit = amountLimit;
        }

        public BigDecimal getSpentAmount() {
            return spentAmount;
        }

        public void setSpentAmount(BigDecimal spentAmount) {
            this.spentAmount = spentAmount;
        }

        public BigDecimal getRemainingAmount() {
            return remainingAmount;
        }

        public void setRemainingAmount(BigDecimal remainingAmount) {
            this.remainingAmount = remainingAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getBudgetStatus() {
            return budgetStatus;
        }

        public void setBudgetStatus(String budgetStatus) {
            this.budgetStatus = budgetStatus;
        }
    }
}

