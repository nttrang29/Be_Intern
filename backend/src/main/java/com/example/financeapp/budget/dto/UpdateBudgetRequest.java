package com.example.financeapp.budget.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateBudgetRequest {

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

    @Min(value = 0, message = "Ngưỡng cảnh báo phải ≥ 0%")
    @Max(value = 100, message = "Ngưỡng cảnh báo phải ≤ 100%")
    private Double warningThreshold; // Ngưỡng cảnh báo (%)


}

