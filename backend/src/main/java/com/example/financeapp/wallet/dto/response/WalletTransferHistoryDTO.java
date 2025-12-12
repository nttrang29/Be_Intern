package com.example.financeapp.wallet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đại diện lịch sử giao dịch chuyển tiền giữa các ví.
 */
public class WalletTransferHistoryDTO {

    private Long transferId;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private String currencyCode;
    private String originalCurrency;
    private BigDecimal exchangeRate;
    private LocalDateTime transferDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String note;
    private String status;
    private Direction direction;
    private UserInfo creator;
    private WalletEdge fromWallet;
    private WalletEdge toWallet;

    public enum Direction {
        OUTGOING,
        INCOMING,
        INTERNAL
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDateTime transferDate) {
        this.transferDate = transferDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public UserInfo getCreator() {
        return creator;
    }

    public void setCreator(UserInfo creator) {
        this.creator = creator;
    }

    public WalletEdge getFromWallet() {
        return fromWallet;
    }

    public void setFromWallet(WalletEdge fromWallet) {
        this.fromWallet = fromWallet;
    }

    public WalletEdge getToWallet() {
        return toWallet;
    }

    public void setToWallet(WalletEdge toWallet) {
        this.toWallet = toWallet;
    }

    public static class WalletEdge {
        private Long walletId;
        private String walletName;
        private String currencyCode;
        private Boolean deleted;

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

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }
    }

    public static class UserInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String avatar;

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

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }
}
