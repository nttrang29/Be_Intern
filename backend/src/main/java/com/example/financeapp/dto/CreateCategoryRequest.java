package com.example.financeapp.dto;

import jakarta.validation.constraints.*;

public class CreateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không quá 100 ký tự")
    private String categoryName;

    @NotNull(message = "Loại giao dịch không được để trống")
    private Long transactionTypeId;

    @Size(max = 255, message = "Icon không quá 255 ký tự")
    private String icon;

    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;

    // Getters & Setters
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getTransactionTypeId() { return transactionTypeId; }
    public void setTransactionTypeId(Long transactionTypeId) { this.transactionTypeId = transactionTypeId; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
