package com.example.financeapp.reminder.service.impl;

import com.example.financeapp.email.EmailService;
import com.example.financeapp.reminder.dto.ReminderSettingsRequest;
import com.example.financeapp.reminder.dto.ReminderSettingsResponse;
import com.example.financeapp.reminder.entity.UserReminder;
import com.example.financeapp.reminder.repository.UserReminderRepository;
import com.example.financeapp.reminder.service.ReminderService;
import com.example.financeapp.transaction.repository.TransactionRepository;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ReminderServiceImpl implements ReminderService {

    @Autowired
    private UserReminderRepository reminderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private EmailService emailService;

    @Override
    public ReminderSettingsResponse getReminderSettings(Long userId) {
        UserReminder reminder = getOrCreateReminder(userId);
        
        ReminderSettingsResponse response = new ReminderSettingsResponse();
        response.setReminderId(reminder.getReminderId());
        response.setEnabled(reminder.isEnabled());
        response.setReminderTime(reminder.getReminderTime());
        response.setLastReminderDate(reminder.getLastReminderDate());
        
        return response;
    }

    @Override
    @Transactional
    public ReminderSettingsResponse updateReminderSettings(Long userId, ReminderSettingsRequest request) {
        UserReminder reminder = getOrCreateReminder(userId);
        
        // Cập nhật enabled
        if (request.getEnabled() != null) {
            reminder.setEnabled(request.getEnabled());
        }
        
        // Cập nhật reminderTime (nếu có)
        if (request.getReminderTime() != null) {
            reminder.setReminderTime(request.getReminderTime());
        }
        
        reminder = reminderRepository.save(reminder);
        
        ReminderSettingsResponse response = new ReminderSettingsResponse();
        response.setReminderId(reminder.getReminderId());
        response.setEnabled(reminder.isEnabled());
        response.setReminderTime(reminder.getReminderTime());
        response.setLastReminderDate(reminder.getLastReminderDate());
        
        return response;
    }

    @Override
    @Transactional
    public UserReminder getOrCreateReminder(Long userId) {
        return reminderRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
                    
                    UserReminder reminder = new UserReminder();
                    reminder.setUser(user);
                    reminder.setEnabled(true);
                    reminder.setReminderTime(LocalTime.of(20, 0)); // Mặc định 20:00
                    return reminderRepository.save(reminder);
                });
    }

    @Override
    public boolean hasTransactionToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        // Kiểm tra xem có giao dịch nào trong ngày hôm nay không
        return transactionRepository.existsByUser_UserIdAndTransactionDateBetween(
                userId, startOfDay, endOfDay);
    }

    @Override
    public void sendReminder(UserReminder reminder) {
        User user = reminder.getUser();
        
        // Kiểm tra user đã ghi giao dịch trong ngày chưa
        if (hasTransactionToday(user.getUserId())) {
            // Đã ghi rồi, không cần nhắc nhở
            return;
        }
        
        // Gửi email nhắc nhở
        emailService.sendDailyReminderEmail(user.getEmail(), user.getFullName());
        
        // Cập nhật lastReminderDate
        reminder.setLastReminderDate(LocalDate.now());
        reminderRepository.save(reminder);
    }
}

