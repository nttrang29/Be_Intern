
package com.example.financeapp.dto;

import jakarta.validation.constraints.*;

public class CreateWalletRequest {

    @NotBlank(message = "Tên ví không được để trống")
    @Size(max = 100, message = "Tên ví không quá 100 ký tự")
    private String walletName;

    @NotBlank(message = "Loại tiền không được để trống")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Mã tiền tệ phải đúng định dạng ISO (VD: VND, USD)")
    private String currencyCode;

    /**
     * @deprecated Số dư ban đầu luôn là 0. Field này bị ignore trong backend.
     * Để thêm tiền vào ví, hãy tạo transaction "Thu nhập" hoặc chuyển tiền từ ví khác.
     */
    @Deprecated
    @DecimalMin(value = "0.0", inclusive = true, message = "Số dư ban đầu phải ≥ 0")
    private Double initialBalance = 0.0;

    @Size(max = 255, message = "Mô tả không quá 255 ký tự")
    private String description;

    private Boolean setAsDefault;

    private String walletType;
    // Getters and Setters
    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(Double initialBalance) { this.initialBalance = initialBalance; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getSetAsDefault() { return setAsDefault; }
    public void setSetAsDefault(Boolean setAsDefault) { this.setAsDefault = setAsDefault; }

    public String getWalletType() { return walletType; }
    public void setWalletType(String walletType) { this.walletType = walletType; }

}