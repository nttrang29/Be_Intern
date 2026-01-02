package com.example.financeapp.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import jakarta.validation.constraints.Size;

public class UpdateTransactionRequest {

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    @NotNull(message = "Số tiền không được để trống")
    private BigDecimal amount;

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    private String note;

    private String imageUrl;

    // Getters & Setters
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

