package com.example.financeapp.category.entity;

import com.example.financeapp.transaction.entity.TransactionType;
import com.example.financeapp.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TransactionType transactionType;

    // Đổi từ icon → description
    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "provider", "enabled", "role", "googleAccount", "firstLogin", "locked", "deleted", "resetToken", "resetTokenExpiredAt", "createdAt", "updatedAt", "lastActiveAt"})
    private User user;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    // Constructors
    public Category() {}

    public Category(String categoryName, TransactionType transactionType, String description, User user, boolean isSystem) {
        this.categoryName = categoryName;
        this.transactionType = transactionType;
        this.description = description;
        this.user = user;
        this.isSystem = isSystem;
    }

    // Getters & Setters
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @JsonProperty("isSystem")
    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }
}