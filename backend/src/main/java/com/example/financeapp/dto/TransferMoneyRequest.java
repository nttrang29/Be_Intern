package com.example.financeapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO cho request chuyển tiền giữa các ví
 */
public class TransferMoneyRequest {

    @NotNull(message = "Ví nguồn không được để trống")
    private Long fromWalletId;

    @NotNull(message = "Ví đích không được để trống")
    private Long toWalletId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Category không được để trống")
    private Long categoryId; // Category cho giao dịch chuyển tiền

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note; // Ghi chú tùy chọn

    // Constructors
    public TransferMoneyRequest() {
    }

    public TransferMoneyRequest(Long fromWalletId, Long toWalletId, BigDecimal amount, Long categoryId) {
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.categoryId = categoryId;
    }

    public TransferMoneyRequest(Long fromWalletId, Long toWalletId, BigDecimal amount, Long categoryId, String note) {
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.categoryId = categoryId;
        this.note = note;
    }

    // Getters & Setters
    public Long getFromWalletId() {
        return fromWalletId;
    }

    public void setFromWalletId(Long fromWalletId) {
        this.fromWalletId = fromWalletId;
    }

    public Long getToWalletId() {
        return toWalletId;
    }

    public void setToWalletId(Long toWalletId) {
        this.toWalletId = toWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

