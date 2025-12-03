package com.example.financeapp.fund.dto;

import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundStatus;
import com.example.financeapp.fund.entity.FundType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO response cho quỹ tiết kiệm
 */
public class FundResponse {
    private Long fundId;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private Long targetWalletId;
    private String targetWalletName;
    private String currencyCode;
    private FundType fundType;
    private FundStatus status;
    private String fundName;
    private Boolean hasDeadline;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal progressPercentage; // % hoàn thành
    private String frequency;
    private BigDecimal amountPerPeriod;
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
    
    // Reminder
    private Boolean reminderEnabled;
    private String reminderType;
    private LocalTime reminderTime;
    private Integer reminderDayOfWeek;
    private Integer reminderDayOfMonth;
    private Integer reminderMonth;
    private Integer reminderDay;
    
    // Auto Deposit
    private Boolean autoDepositEnabled;
    private String autoDepositType;
    private Long sourceWalletId;
    private String sourceWalletName;
    private String autoDepositScheduleType;
    private LocalTime autoDepositTime;
    private Integer autoDepositDayOfWeek;
    private Integer autoDepositDayOfMonth;
    private Integer autoDepositMonth;
    private Integer autoDepositDay;
    private BigDecimal autoDepositAmount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thành viên (cho quỹ nhóm)
    private List<FundMemberResponse> members;
    private Integer memberCount;

    public static FundResponse fromEntity(Fund fund) {
        FundResponse response = new FundResponse();
        response.setFundId(fund.getFundId());
        response.setOwnerId(fund.getOwner().getUserId());
        response.setOwnerName(fund.getOwner().getFullName());
        response.setOwnerEmail(fund.getOwner().getEmail());
        response.setTargetWalletId(fund.getTargetWallet().getWalletId());
        response.setTargetWalletName(fund.getTargetWallet().getWalletName());
        response.setCurrencyCode(fund.getTargetWallet().getCurrencyCode());
        response.setFundType(fund.getFundType());
        response.setStatus(fund.getStatus());
        response.setFundName(fund.getFundName());
        response.setHasDeadline(fund.getHasDeadline());
        response.setTargetAmount(fund.getTargetAmount());
        response.setCurrentAmount(fund.getCurrentAmount());
        
        // Tính % hoàn thành
        if (fund.getHasDeadline() && fund.getTargetAmount() != null && 
            fund.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = fund.getCurrentAmount()
                    .divide(fund.getTargetAmount(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            response.setProgressPercentage(percentage);
        } else {
            response.setProgressPercentage(BigDecimal.ZERO);
        }
        
        response.setFrequency(fund.getFrequency() != null ? fund.getFrequency().name() : null);
        response.setAmountPerPeriod(fund.getAmountPerPeriod());
        response.setStartDate(fund.getStartDate());
        response.setEndDate(fund.getEndDate());
        response.setNote(fund.getNote());
        
        // Reminder
        response.setReminderEnabled(fund.getReminderEnabled());
        response.setReminderType(fund.getReminderType() != null ? fund.getReminderType().name() : null);
        response.setReminderTime(fund.getReminderTime());
        response.setReminderDayOfWeek(fund.getReminderDayOfWeek());
        response.setReminderDayOfMonth(fund.getReminderDayOfMonth());
        response.setReminderMonth(fund.getReminderMonth());
        response.setReminderDay(fund.getReminderDay());
        
        // Auto Deposit
        response.setAutoDepositEnabled(fund.getAutoDepositEnabled());
        // autoDepositType không còn sử dụng nữa (để null)
        response.setAutoDepositType(null);
        if (fund.getSourceWallet() != null) {
            response.setSourceWalletId(fund.getSourceWallet().getWalletId());
            response.setSourceWalletName(fund.getSourceWallet().getWalletName());
        }
        response.setAutoDepositScheduleType(fund.getAutoDepositScheduleType() != null ? fund.getAutoDepositScheduleType().name() : null);
        response.setAutoDepositTime(fund.getAutoDepositTime());
        response.setAutoDepositDayOfWeek(fund.getAutoDepositDayOfWeek());
        response.setAutoDepositDayOfMonth(fund.getAutoDepositDayOfMonth());
        response.setAutoDepositMonth(fund.getAutoDepositMonth());
        response.setAutoDepositDay(fund.getAutoDepositDay());
        response.setAutoDepositAmount(fund.getAutoDepositAmount());
        
        response.setCreatedAt(fund.getCreatedAt());
        response.setUpdatedAt(fund.getUpdatedAt());
        
        return response;
    }

    // Getters & Setters
    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public Long getTargetWalletId() { return targetWalletId; }
    public void setTargetWalletId(Long targetWalletId) { this.targetWalletId = targetWalletId; }

    public String getTargetWalletName() { return targetWalletName; }
    public void setTargetWalletName(String targetWalletName) { this.targetWalletName = targetWalletName; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public FundType getFundType() { return fundType; }
    public void setFundType(FundType fundType) { this.fundType = fundType; }

    public FundStatus getStatus() { return status; }
    public void setStatus(FundStatus status) { this.status = status; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public Boolean getHasDeadline() { return hasDeadline; }
    public void setHasDeadline(Boolean hasDeadline) { this.hasDeadline = hasDeadline; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

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

    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }

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

    public String getAutoDepositType() { return autoDepositType; }
    public void setAutoDepositType(String autoDepositType) { this.autoDepositType = autoDepositType; }

    public Long getSourceWalletId() { return sourceWalletId; }
    public void setSourceWalletId(Long sourceWalletId) { this.sourceWalletId = sourceWalletId; }

    public String getSourceWalletName() { return sourceWalletName; }
    public void setSourceWalletName(String sourceWalletName) { this.sourceWalletName = sourceWalletName; }

    public String getAutoDepositScheduleType() { return autoDepositScheduleType; }
    public void setAutoDepositScheduleType(String autoDepositScheduleType) { this.autoDepositScheduleType = autoDepositScheduleType; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<FundMemberResponse> getMembers() { return members; }
    public void setMembers(List<FundMemberResponse> members) { this.members = members; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
}

