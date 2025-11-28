package com.example.financeapp.scheduledtransaction.dto;

import com.example.financeapp.scheduledtransaction.entity.ScheduleType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO để tạo scheduled transaction
 */
public class CreateScheduledTransactionRequest {

    @NotNull(message = "Vui lòng chọn ví")
    private Long walletId;

    @NotNull(message = "Vui lòng chọn loại giao dịch")
    private Long transactionTypeId; // 1 = Chi tiêu, 2 = Thu nhập

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    private String note;

    @NotNull(message = "Vui lòng chọn kiểu lịch hẹn")
    private ScheduleType scheduleType;

    @NotNull(message = "Vui lòng chọn ngày thực hiện")
    private LocalDate startDate; // Ngày bắt đầu (cho ONCE) hoặc ngày đầu tiên (cho định kỳ)

    @NotNull(message = "Vui lòng chọn giờ thực hiện")
    private LocalTime executionTime;

    private LocalDate endDate; // null = không giới hạn

    // Các trường cho từng loại lịch
    @Min(value = 1, message = "Thứ trong tuần phải từ 1-7")
    @Max(value = 7, message = "Thứ trong tuần phải từ 1-7")
    private Integer dayOfWeek; // 1-7 (Monday-Sunday) cho WEEKLY

    @Min(value = 1, message = "Ngày trong tháng phải từ 1-31")
    @Max(value = 31, message = "Ngày trong tháng phải từ 1-31")
    private Integer dayOfMonth; // 1-31 cho MONTHLY

    @Min(value = 1, message = "Tháng phải từ 1-12")
    @Max(value = 12, message = "Tháng phải từ 1-12")
    private Integer month; // 1-12 cho YEARLY

    @Min(value = 1, message = "Ngày phải từ 1-31")
    @Max(value = 31, message = "Ngày phải từ 1-31")
    private Integer day; // 1-31 cho YEARLY

    // Getters & Setters
    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public Long getTransactionTypeId() { return transactionTypeId; }
    public void setTransactionTypeId(Long transactionTypeId) { this.transactionTypeId = transactionTypeId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalTime getExecutionTime() { return executionTime; }
    public void setExecutionTime(LocalTime executionTime) { this.executionTime = executionTime; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }
}

