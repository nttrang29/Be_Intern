package com.example.financeapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * DTO cho request cập nhật thông tin ví
 * 
 * Cho phép sửa:
 * - walletName: Tên ví (bắt buộc)
 * - description: Mô tả (tùy chọn)
 * - balance: Số dư (CHỈ khi ví chưa có giao dịch nào)
 * 
 * KHÔNG cho phép sửa:
 * - currencyCode: Loại tiền tệ (immutable)
 * - balance: Nếu ví đã có giao dịch (chỉ thay đổi qua transactions hoặc xóa ví)
 */
public class UpdateWalletRequest {

    @NotBlank(message = "Tên ví không được để trống")
    @Size(max = 100, message = "Tên ví không được vượt quá 100 ký tự")
    private String walletName;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    @DecimalMin(value = "0.0", message = "Số dư không được âm")
    private BigDecimal balance; // Tùy chọn - chỉ cho phép sửa nếu chưa có transactions

    // Constructors
    public UpdateWalletRequest() {
    }

    public UpdateWalletRequest(String walletName, String description) {
        this.walletName = walletName;
        this.description = description;
    }

    public UpdateWalletRequest(String walletName, String description, BigDecimal balance) {
        this.walletName = walletName;
        this.description = description;
        this.balance = balance;
    }

    // Getters & Setters
    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

