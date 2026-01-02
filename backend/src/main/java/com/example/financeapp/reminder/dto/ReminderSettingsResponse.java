package com.example.financeapp.reminder.dto;

import java.time.LocalTime;

/**
 * DTO response cho cấu hình nhắc nhở
 */
public class ReminderSettingsResponse {
    private Long reminderId;
    private boolean enabled;
    private LocalTime reminderTime;
    private java.time.LocalDate lastReminderDate;

    public ReminderSettingsResponse() {}

    // Getters & Setters
    public Long getReminderId() {
        return reminderId;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
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
}

