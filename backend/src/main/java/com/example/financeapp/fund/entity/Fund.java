package com.example.financeapp.fund.entity;

import com.example.financeapp.user.entity.User;
import com.example.financeapp.wallet.entity.Wallet;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity cho quỹ tiết kiệm
 */
@Entity
@Table(name = "funds")
public class Fund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fund_id")
    private Long fundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner; // Chủ quỹ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet targetWallet; // Ví đích (ví quỹ)

    @Enumerated(EnumType.STRING)
    @Column(name = "fund_type", nullable = false, length = 20)
    private FundType fundType; // PERSONAL hoặc GROUP

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FundStatus status = FundStatus.ACTIVE;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "pending_auto_topup_amount", precision = 20, scale = 8)
    private BigDecimal pendingAutoTopupAmount = BigDecimal.ZERO;

    @Column(name = "pending_auto_topup_at")
    private LocalDateTime pendingAutoTopupAt;

    @Column(name = "fund_name", nullable = false, length = 200)
    private String fundName;

    @Column(name = "has_deadline", nullable = false)
    private Boolean hasDeadline = true; // Có kỳ hạn hay không

    @Column(name = "target_amount", precision = 20, scale = 8)
    private BigDecimal targetAmount; // Số tiền mục tiêu (null nếu không kỳ hạn)

    @Column(name = "current_amount", precision = 20, scale = 8)
    private BigDecimal currentAmount = BigDecimal.ZERO; // Số tiền hiện có

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 20)
    private FundFrequency frequency; // Tần suất gửi quỹ

    @Column(name = "amount_per_period", precision = 20, scale = 8)
    private BigDecimal amountPerPeriod; // Số tiền gửi mỗi kỳ

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ============ NHẮC NHỞ ============
    @Column(name = "reminder_enabled")
    private Boolean reminderEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", length = 20)
    private ReminderType reminderType;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "reminder_day_of_week")
    private Integer reminderDayOfWeek; // 1-7 cho WEEKLY

    @Column(name = "reminder_day_of_month")
    private Integer reminderDayOfMonth; // 1-31 cho MONTHLY

    @Column(name = "reminder_month")
    private Integer reminderMonth; // 1-12 cho YEARLY

    @Column(name = "reminder_day")
    private Integer reminderDay; // 1-31 cho YEARLY

    // ============ TỰ ĐỘNG NẠP TIỀN ============
    @Column(name = "auto_deposit_enabled")
    private Boolean autoDepositEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_deposit_type", length = 20)
    private AutoDepositType autoDepositType; // FOLLOW_REMINDER hoặc CUSTOM_SCHEDULE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id")
    private Wallet sourceWallet; // Ví nguồn (để tự động nạp)

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_deposit_schedule_type", length = 20)
    private ReminderType autoDepositScheduleType; // Cho CUSTOM_SCHEDULE

    @Column(name = "auto_deposit_time")
    private LocalTime autoDepositTime;

    @Column(name = "auto_deposit_day_of_week")
    private Integer autoDepositDayOfWeek;

    @Column(name = "auto_deposit_day_of_month")
    private Integer autoDepositDayOfMonth;

    @Column(name = "auto_deposit_month")
    private Integer autoDepositMonth;

    @Column(name = "auto_deposit_day")
    private Integer autoDepositDay;

    @Column(name = "auto_deposit_amount", precision = 20, scale = 8)
    private BigDecimal autoDepositAmount; // Số tiền mỗi lần nạp

    @Column(name = "auto_deposit_start_at")
    private LocalDateTime autoDepositStartAt; // Thời điểm bắt đầu chạy auto-deposit

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public Wallet getTargetWallet() { return targetWallet; }
    public void setTargetWallet(Wallet targetWallet) { this.targetWallet = targetWallet; }

    public FundType getFundType() { return fundType; }
    public void setFundType(FundType fundType) { this.fundType = fundType; }

    public FundStatus getStatus() { return status; }
    public void setStatus(FundStatus status) { this.status = status; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public BigDecimal getPendingAutoTopupAmount() { return pendingAutoTopupAmount; }
    public void setPendingAutoTopupAmount(BigDecimal pendingAutoTopupAmount) { this.pendingAutoTopupAmount = pendingAutoTopupAmount; }

    public LocalDateTime getPendingAutoTopupAt() { return pendingAutoTopupAt; }
    public void setPendingAutoTopupAt(LocalDateTime pendingAutoTopupAt) { this.pendingAutoTopupAt = pendingAutoTopupAt; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public Boolean getHasDeadline() { return hasDeadline; }
    public void setHasDeadline(Boolean hasDeadline) { this.hasDeadline = hasDeadline; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

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

    // Reminder getters & setters
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

    // Auto deposit getters & setters
    public Boolean getAutoDepositEnabled() { return autoDepositEnabled; }
    public void setAutoDepositEnabled(Boolean autoDepositEnabled) { this.autoDepositEnabled = autoDepositEnabled; }

    public AutoDepositType getAutoDepositType() { return autoDepositType; }
    public void setAutoDepositType(AutoDepositType autoDepositType) { this.autoDepositType = autoDepositType; }

    public Wallet getSourceWallet() { return sourceWallet; }
    public void setSourceWallet(Wallet sourceWallet) { this.sourceWallet = sourceWallet; }

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

    public LocalDateTime getAutoDepositStartAt() { return autoDepositStartAt; }
    public void setAutoDepositStartAt(LocalDateTime autoDepositStartAt) { this.autoDepositStartAt = autoDepositStartAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

