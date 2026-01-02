package com.example.financeapp.reminder.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * DTO để cập nhật cấu hình nhắc nhở
 */
public class ReminderSettingsRequest {

    @NotNull(message = "Trạng thái bật/tắt không được để trống")
    private Boolean enabled;

    private LocalTime reminderTime; // null = giữ nguyên

    // Getters & Setters
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }
}

