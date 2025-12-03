package com.example.financeapp.transaction.entity;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.wallet.entity.Wallet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private TransactionType transactionType;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    // ============ FIELDS CHO CURRENCY CONVERSION (Merge Wallet) ============

    @Column(name = "original_amount", precision = 20, scale = 8)
    private BigDecimal originalAmount; // Số tiền gốc trước khi chuyển đổi (nếu có)

    @Column(name = "original_currency", length = 3)
    private String originalCurrency; // Loại tiền gốc (nếu có chuyển đổi)

    @Column(name = "exchange_rate", precision = 20, scale = 6)
    private BigDecimal exchangeRate; // Tỷ giá áp dụng (nếu có chuyển đổi)

    @Column(name = "merge_date")
    private LocalDateTime mergeDate; // Ngày gộp ví (để biết transaction từ merge)

    // ============ FIELDS CHO BUDGET EXCEEDED ============
    
    @Column(name = "is_exceeded_budget")
    private Boolean isExceededBudget = false; // Đánh dấu giao dịch vượt hạn mức ngân sách
    
    @Column(name = "exceeded_budget_amount", precision = 20, scale = 8)
    private BigDecimal exceededBudgetAmount = BigDecimal.ZERO; // Số tiền vượt hạn mức (nếu có)
    
    @Column(name = "exceeded_budget_id")
    private Long exceededBudgetId; // ID của budget bị vượt (nếu có)

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public String getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(String originalCurrency) { this.originalCurrency = originalCurrency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public LocalDateTime getMergeDate() { return mergeDate; }
    public void setMergeDate(LocalDateTime mergeDate) { this.mergeDate = mergeDate; }

    public Boolean getIsExceededBudget() { return isExceededBudget; }
    public void setIsExceededBudget(Boolean isExceededBudget) { this.isExceededBudget = isExceededBudget; }

    public BigDecimal getExceededBudgetAmount() { return exceededBudgetAmount; }
    public void setExceededBudgetAmount(BigDecimal exceededBudgetAmount) { this.exceededBudgetAmount = exceededBudgetAmount; }

    public Long getExceededBudgetId() { return exceededBudgetId; }
    public void setExceededBudgetId(Long exceededBudgetId) { this.exceededBudgetId = exceededBudgetId; }
}