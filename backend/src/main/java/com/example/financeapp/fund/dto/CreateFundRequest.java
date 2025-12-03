package com.example.financeapp.fund.dto;

import com.example.financeapp.fund.entity.AutoDepositType;
import com.example.financeapp.fund.entity.FundFrequency;
import com.example.financeapp.fund.entity.FundType;
import com.example.financeapp.fund.entity.ReminderType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO để tạo quỹ tiết kiệm
 */
public class CreateFundRequest {

    @NotNull(message = "Tên quỹ không được để trống")
    @Size(max = 200, message = "Tên quỹ không quá 200 ký tự")
    private String fundName;

    @NotNull(message = "Vui lòng chọn ví nguồn")
    private Long sourceWalletId; // Ví nguồn để nạp tiền vào quỹ

    @NotNull(message = "Vui lòng chọn loại quỹ")
    private FundType fundType; // PERSONAL hoặc GROUP

    @NotNull(message = "Vui lòng chọn loại kỳ hạn")
    private Boolean hasDeadline; // true = có kỳ hạn, false = không kỳ hạn

    // Các trường cho quỹ có kỳ hạn
    private BigDecimal targetAmount; // Bắt buộc nếu hasDeadline = true
    private FundFrequency frequency; // Bắt buộc nếu hasDeadline = true
    private BigDecimal amountPerPeriod;
    private LocalDate startDate; // Bắt buộc nếu hasDeadline = true
    private LocalDate endDate; // Bắt buộc nếu hasDeadline = true

    // Các trường cho quỹ không kỳ hạn (tùy chọn)
    // frequency, amountPerPeriod, startDate có thể có hoặc không

    // Nhắc nhở
    private Boolean reminderEnabled = false;
    private ReminderType reminderType;
    private LocalTime reminderTime;
    private Integer reminderDayOfWeek; // 1-7 cho WEEKLY
    private Integer reminderDayOfMonth; // 1-31 cho MONTHLY
    private Integer reminderMonth; // 1-12 cho YEARLY
    private Integer reminderDay; // 1-31 cho YEARLY

    // Tự động nạp tiền (theo tần suất của quỹ)
    private Boolean autoDepositEnabled = false;
    private ReminderType autoDepositScheduleType; // DAILY, WEEKLY, MONTHLY, YEARLY (theo frequency)
    private LocalTime autoDepositTime;
    private Integer autoDepositDayOfWeek;
    private Integer autoDepositDayOfMonth;
    private Integer autoDepositMonth;
    private Integer autoDepositDay;
    private BigDecimal autoDepositAmount; // Số tiền tự động nạp mỗi lần

    private String note;

    // Thành viên quỹ nhóm (chỉ cho GROUP)
    private List<FundMemberRequest> members;

    public static class FundMemberRequest {
        @NotBlank(message = "Email thành viên không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;

        @NotNull(message = "Vui lòng chọn quyền thành viên")
        private String role; // "OWNER" hoặc "CONTRIBUTOR"

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    // Getters & Setters
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public Long getSourceWalletId() { return sourceWalletId; }
    public void setSourceWalletId(Long sourceWalletId) { this.sourceWalletId = sourceWalletId; }

    public FundType getFundType() { return fundType; }
    public void setFundType(FundType fundType) { this.fundType = fundType; }

    public Boolean getHasDeadline() { return hasDeadline; }
    public void setHasDeadline(Boolean hasDeadline) { this.hasDeadline = hasDeadline; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public FundFrequency getFrequency() { return frequency; }
    public void setFrequency(FundFrequency frequency) { this.frequency = frequency; }

    public BigDecimal getAmountPerPeriod() { return amountPerPeriod; }
    public void setAmountPerPeriod(BigDecimal amountPerPeriod) { this.amountPerPeriod = amountPerPeriod; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

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

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<FundMemberRequest> getMembers() { return members; }
    public void setMembers(List<FundMemberRequest> members) { this.members = members; }
}

