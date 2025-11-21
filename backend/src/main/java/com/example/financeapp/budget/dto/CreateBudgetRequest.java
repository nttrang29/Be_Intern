package com.example.financeapp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateBudgetRequest {

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    private Long walletId; // null = áp dụng cho tất cả ví

    @NotNull(message = "Hạn mức không được để trống")
    @DecimalMin(value = "1000", message = "Hạn mức phải ≥ 1.000 VND")
    private BigDecimal amountLimit;

    @NotNull(message = "Chọn ngày bắt đầu")
    private LocalDate startDate;

    @NotNull(message = "Chọn ngày kết thúc")
    private LocalDate endDate;

    @Size(max = 255, message = "Ghi chú không quá 255 ký tự")
    private String note;

    // Getters & Setters
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public BigDecimal getAmountLimit() { return amountLimit; }
    public void setAmountLimit(BigDecimal amountLimit) { this.amountLimit = amountLimit; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}