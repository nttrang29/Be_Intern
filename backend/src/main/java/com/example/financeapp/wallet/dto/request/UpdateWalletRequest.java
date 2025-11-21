package com.example.financeapp.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWalletRequest {

    @NotBlank(message = "Tên ví không được để trống")
    @Size(max = 100, message = "Tên ví không được vượt quá 100 ký tự")
    private String walletName;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    // currencyCode KHÔNG ĐƯỢC SỬA → nhưng vẫn cần để client gửi lên (nếu muốn kiểm tra)
    private String currencyCode;

    @DecimalMin(value = "0.0", message = "Số dư không được âm")
    private BigDecimal balance;

    private Boolean setAsDefault;

    // Cho phép chuyển đổi loại ví: PERSONAL -> GROUP (không cho phép GROUP -> PERSONAL)
    private String walletType; // "PERSONAL" hoặc "GROUP"
}
