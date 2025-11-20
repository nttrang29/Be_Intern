package com.example.financeapp.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BudgetDTO {
    private Long id;
    private String name;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long walletId;
    private String category;

    // tổng chi tiêu thực tế
    private BigDecimal totalSpending;

    // % mức độ sử dụng
    private double usagePercent;
}
