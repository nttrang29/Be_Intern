package com.example.financeapp.reminder.service;

import com.example.financeapp.reminder.dto.ReminderSettingsRequest;
import com.example.financeapp.reminder.dto.ReminderSettingsResponse;
import com.example.financeapp.reminder.entity.UserReminder;

/**
 * Service để quản lý nhắc nhở ghi giao dịch
 */
public interface ReminderService {
    
    /**
     * Lấy cấu hình nhắc nhở của user
     */
    ReminderSettingsResponse getReminderSettings(Long userId);
    
    /**
     * Cập nhật cấu hình nhắc nhở
     */
    ReminderSettingsResponse updateReminderSettings(Long userId, ReminderSettingsRequest request);
    
    /**
     * Tạo hoặc lấy reminder của user (nếu chưa có thì tạo mới)
     */
    UserReminder getOrCreateReminder(Long userId);
    
    /**
     * Kiểm tra user đã ghi giao dịch trong ngày chưa
     */
    boolean hasTransactionToday(Long userId);
    
    /**
     * Gửi nhắc nhở cho user
     */
    void sendReminder(UserReminder reminder);
}

