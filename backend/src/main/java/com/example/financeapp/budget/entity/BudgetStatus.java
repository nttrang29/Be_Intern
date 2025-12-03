package com.example.financeapp.budget.entity;

/**
 * Enum cho trạng thái ngân sách
 */
public enum BudgetStatus {
    PENDING,     // Đang chờ (chưa đến ngày bắt đầu)
    ACTIVE,      // Đang hoạt động trong hạn mức
    WARNING,     // Đang hoạt động nhưng gần chạm ngưỡng cảnh báo
    EXCEEDED,    // Vượt hạn mức
    COMPLETED    // Hoàn thành (hết thời gian, không vượt hạn mức)
}

