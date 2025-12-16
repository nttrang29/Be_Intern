package com.example.financeapp.wallet.entity;

import com.example.financeapp.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_name", nullable = false, length = 100)
    private String walletName;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "balance", precision = 20, scale = 8)
    private BigDecimal balance = BigDecimal.ZERO;

    // Lưu số dư gốc để đảm bảo tính đối xứng khi chuyển đổi tiền tệ (A->B->A = A)
    @Column(name = "original_balance", precision = 20, scale = 8)
    private BigDecimal originalBalance;

    @Column(name = "original_currency", length = 3)
    private String originalCurrency;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_default")
    @JsonProperty("isDefault")
    private boolean isDefault = false;

    @Column(name = "is_fund_wallet")
    @JsonProperty("isFundWallet")
    private boolean isFundWallet = false; // ✨ NEW: Đánh dấu ví quỹ (tự động tạo khi tạo fund)

    // NEW: Phân loại ví (Cá nhân / Nhóm)
    @Column(name = "wallet_type", length = 20)
    private String walletType = "PERSONAL"; // PERSONAL hoặc GROUP

    // ===== Soft delete =====
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============ GETTERS & SETTERS ============

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    public BigDecimal getOriginalBalance() {
        return originalBalance;
    }

    public void setOriginalBalance(BigDecimal originalBalance) {
        this.originalBalance = originalBalance;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isFundWallet() {
        return isFundWallet;
    }

    public void setFundWallet(boolean fundWallet) {
        isFundWallet = fundWallet;
    }
}
