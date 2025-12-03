package com.example.financeapp.fund.dto;

import com.example.financeapp.fund.entity.FundFrequency;
import com.example.financeapp.fund.entity.ReminderType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO để cập nhật quỹ
 */
public class UpdateFundRequest {
    private String fundName;
    private FundFrequency frequency;
    private BigDecimal amountPerPeriod;
    private LocalDate startDate;
    private LocalDate endDate; // Chỉ cho quỹ có kỳ hạn
    private String note;
    
    // Reminder
    private Boolean reminderEnabled;
    private ReminderType reminderType;
    private LocalTime reminderTime;
    private Integer reminderDayOfWeek;
    private Integer reminderDayOfMonth;
    private Integer reminderMonth;
    private Integer reminderDay;
    
    // Auto Deposit (sourceWallet không thể thay đổi sau khi tạo)
    private Boolean autoDepositEnabled;
    private ReminderType autoDepositScheduleType;
    private LocalTime autoDepositTime;
    private Integer autoDepositDayOfWeek;
    private Integer autoDepositDayOfMonth;
    private Integer autoDepositMonth;
    private Integer autoDepositDay;
    private BigDecimal autoDepositAmount;
    
    // Thành viên (cho quỹ nhóm)
    private List<FundMemberRequest> members;
    
    public static class FundMemberRequest {
        private String email;
        private String role; // "OWNER" hoặc "CONTRIBUTOR"
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    // Getters & Setters
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public FundFrequency getFrequency() { return frequency; }
    public void setFrequency(FundFrequency frequency) { this.frequency = frequency; }

    public BigDecimal getAmountPerPeriod() { return amountPerPeriod; }
    public void setAmountPerPeriod(BigDecimal amountPerPeriod) { this.amountPerPeriod = amountPerPeriod; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Boolean getReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(Boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

    public ReminderType getReminderType() { return reminderType; }
    public void setReminderType(ReminderType reminderType) { this.reminderType = reminderType; }

    public LocalTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalTime reminderTime) { this.reminderTime = reminderTime; }

    public Integer getReminderDayOfWeek() { return reminderDayOfWeek; }
    public void setReminderDayOfWeek(Integer reminderDayOfWeek) { this.reminderDayOfWeek = reminderDayOfWeek; }

    public Integer getReminderDayOfMonth() { return reminderDayOfMonth; }
    public void setReminderDayOfMonth(Integer reminderDayOfMonth) { this.reminderDayOfMonth = reminderDayOfMonth; }

    public Integer getReminderMonth() { return reminderMonth; }
    public void setReminderMonth(Integer reminderMonth) { this.reminderMonth = reminderMonth; }

    public Integer getReminderDay() { return reminderDay; }
    public void setReminderDay(Integer reminderDay) { this.reminderDay = reminderDay; }

    public Boolean getAutoDepositEnabled() { return autoDepositEnabled; }
    public void setAutoDepositEnabled(Boolean autoDepositEnabled) { this.autoDepositEnabled = autoDepositEnabled; }

    public ReminderType getAutoDepositScheduleType() { return autoDepositScheduleType; }
    public void setAutoDepositScheduleType(ReminderType autoDepositScheduleType) { this.autoDepositScheduleType = autoDepositScheduleType; }

    public LocalTime getAutoDepositTime() { return autoDepositTime; }
    public void setAutoDepositTime(LocalTime autoDepositTime) { this.autoDepositTime = autoDepositTime; }

    public Integer getAutoDepositDayOfWeek() { return autoDepositDayOfWeek; }
    public void setAutoDepositDayOfWeek(Integer autoDepositDayOfWeek) { this.autoDepositDayOfWeek = autoDepositDayOfWeek; }

    public Integer getAutoDepositDayOfMonth() { return autoDepositDayOfMonth; }
    public void setAutoDepositDayOfMonth(Integer autoDepositDayOfMonth) { this.autoDepositDayOfMonth = autoDepositDayOfMonth; }

    public Integer getAutoDepositMonth() { return autoDepositMonth; }
    public void setAutoDepositMonth(Integer autoDepositMonth) { this.autoDepositMonth = autoDepositMonth; }

    public Integer getAutoDepositDay() { return autoDepositDay; }
    public void setAutoDepositDay(Integer autoDepositDay) { this.autoDepositDay = autoDepositDay; }

    public BigDecimal getAutoDepositAmount() { return autoDepositAmount; }
    public void setAutoDepositAmount(BigDecimal autoDepositAmount) { this.autoDepositAmount = autoDepositAmount; }

    public List<FundMemberRequest> getMembers() { return members; }
    public void setMembers(List<FundMemberRequest> members) { this.members = members; }
}

