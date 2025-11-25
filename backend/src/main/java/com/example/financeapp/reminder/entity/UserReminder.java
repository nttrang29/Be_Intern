package com.example.financeapp.reminder.entity;

import com.example.financeapp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * Entity để lưu cấu hình nhắc nhở ghi giao dịch của user
 */
@Entity
@Table(name = "user_reminders")
public class UserReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Long reminderId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true; // Mặc định bật nhắc nhở

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime = LocalTime.of(20, 0); // Mặc định 20:00

    @Column(name = "last_reminder_date")
    private java.time.LocalDate lastReminderDate; // Ngày gửi nhắc nhở cuối cùng

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }

    // Getters & Setters
    public Long getReminderId() {
        return reminderId;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public java.time.LocalDate getLastReminderDate() {
        return lastReminderDate;
    }

    public void setLastReminderDate(java.time.LocalDate lastReminderDate) {
        this.lastReminderDate = lastReminderDate;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

