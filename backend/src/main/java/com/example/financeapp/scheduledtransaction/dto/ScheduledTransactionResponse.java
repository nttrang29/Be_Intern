package com.example.financeapp.scheduledtransaction.dto;

import com.example.financeapp.scheduledtransaction.entity.ScheduledTransaction;
import com.example.financeapp.scheduledtransaction.entity.ScheduleStatus;
import com.example.financeapp.scheduledtransaction.entity.ScheduleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO response cho scheduled transaction
 */
public class ScheduledTransactionResponse {
    private Long scheduleId;
    private Long walletId;
    private String walletName;
    private Long transactionTypeId;
    private String transactionTypeName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String note;
    private ScheduleType scheduleType;
    private ScheduleStatus status;
    private LocalDate nextExecutionDate;
    private LocalTime executionTime;
    private LocalDate endDate;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private Integer month;
    private Integer day;
    private Integer completedCount;
    private Integer failedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduledTransactionResponse() {}

    public static ScheduledTransactionResponse fromEntity(ScheduledTransaction st) {
        ScheduledTransactionResponse response = new ScheduledTransactionResponse();
        response.setScheduleId(st.getScheduleId());
        response.setWalletId(st.getWallet().getWalletId());
        response.setWalletName(st.getWallet().getWalletName());
        response.setTransactionTypeId(st.getTransactionType().getTypeId());
        response.setTransactionTypeName(st.getTransactionType().getTypeName());
        response.setCategoryId(st.getCategory().getCategoryId());
        response.setCategoryName(st.getCategory().getCategoryName());
        response.setAmount(st.getAmount());
        response.setNote(st.getNote());
        response.setScheduleType(st.getScheduleType());
        response.setStatus(st.getStatus());
        response.setNextExecutionDate(st.getNextExecutionDate());
        response.setExecutionTime(st.getExecutionTime());
        response.setEndDate(st.getEndDate());
        response.setDayOfWeek(st.getDayOfWeek());
        response.setDayOfMonth(st.getDayOfMonth());
        response.setMonth(st.getMonth());
        response.setDay(st.getDay());
        response.setCompletedCount(st.getCompletedCount());
        response.setFailedCount(st.getFailedCount());
        response.setCreatedAt(st.getCreatedAt());
        response.setUpdatedAt(st.getUpdatedAt());
        return response;
    }

    // Getters & Setters
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }

    public Long getTransactionTypeId() { return transactionTypeId; }
    public void setTransactionTypeId(Long transactionTypeId) { this.transactionTypeId = transactionTypeId; }

    public String getTransactionTypeName() { return transactionTypeName; }
    public void setTransactionTypeName(String transactionTypeName) { this.transactionTypeName = transactionTypeName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public ScheduleStatus getStatus() { return status; }
    public void setStatus(ScheduleStatus status) { this.status = status; }

    public LocalDate getNextExecutionDate() { return nextExecutionDate; }
    public void setNextExecutionDate(LocalDate nextExecutionDate) { this.nextExecutionDate = nextExecutionDate; }

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

    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }

    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

