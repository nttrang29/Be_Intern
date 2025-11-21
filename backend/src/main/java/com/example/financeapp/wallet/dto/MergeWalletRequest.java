package com.example.financeapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO cho request gộp ví
 * Hỗ trợ merge ví khác loại tiền tệ với currency conversion
 */
public class MergeWalletRequest {

    @NotNull(message = "Ví nguồn không được để trống")
    private Long sourceWalletId;

    @NotNull(message = "Loại tiền đích không được để trống")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Mã tiền tệ phải đúng định dạng ISO (VD: VND, USD)")
    private String targetCurrency; // Loại tiền tệ sau khi gộp (VD: "VND", "USD")

    // Constructors
    public MergeWalletRequest() {}

    public MergeWalletRequest(Long sourceWalletId, String targetCurrency) {
        this.sourceWalletId = sourceWalletId;
        this.targetCurrency = targetCurrency;
    }

    // Getters & Setters
    public Long getSourceWalletId() {
        return sourceWalletId;
    }

    public void setSourceWalletId(Long sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }
}

