package com.example.financeapp.scheduledtransaction.entity;

/**
 * Enum cho trạng thái lịch giao dịch
 */
public enum ScheduleStatus {
    PENDING,   // Đang chờ đến thời gian thực hiện
    COMPLETED, // Đã thực hiện thành công
    FAILED     // Thất bại (thường do không đủ tiền)
}

