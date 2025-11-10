package com.example.financeapp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateTransactionRequest {

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Ngày giao dịch không được để trống")
    private LocalDateTime transactionDate;

    @NotNull(message = "Ví không được để trống")
    private Long walletId;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    private String note;

    private String imageUrl;

    // Getters & Setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}