package com.example.financeapp.reminder.scheduler;

import com.example.financeapp.reminder.entity.UserReminder;
import com.example.financeapp.reminder.repository.UserReminderRepository;
import com.example.financeapp.reminder.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled task để gửi nhắc nhở ghi giao dịch hàng ngày
 * Chạy mỗi 30 phút để kiểm tra user nào cần nhắc nhở
 */
@Component
public class DailyReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReminderScheduler.class);

    @Autowired
    private UserReminderRepository reminderRepository;
    
    @Autowired
    private ReminderService reminderService;

    /**
     * Chạy mỗi 30 phút để kiểm tra và gửi nhắc nhở
     * Cron expression: 0 0,30 * * * * (mỗi 30 phút - vào phút 0 và 30 của mỗi giờ)
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void sendDailyReminders() {
        log.info("Bắt đầu kiểm tra nhắc nhở ghi giao dịch...");
        
        LocalTime currentTime = LocalTime.now();
        LocalDate today = LocalDate.now();
        
        // Tìm tất cả user cần nhắc nhở
        List<UserReminder> remindersToSend = reminderRepository.findUsersToRemind(currentTime, today);
        
        log.info("Tìm thấy {} user cần nhắc nhở", remindersToSend.size());
        
        int successCount = 0;
        int skipCount = 0;
        
        for (UserReminder reminder : remindersToSend) {
            try {
                // Kiểm tra lại xem user đã ghi giao dịch chưa (tránh race condition)
                if (reminderService.hasTransactionToday(reminder.getUser().getUserId())) {
                    // Đã ghi rồi, cập nhật lastReminderDate để không gửi lại
                    reminder.setLastReminderDate(today);
                    reminderRepository.save(reminder);
                    skipCount++;
                    continue;
                }
                
                // Gửi nhắc nhở
                reminderService.sendReminder(reminder);
                successCount++;
                
                log.info("Đã gửi nhắc nhở cho user: {}", reminder.getUser().getEmail());
                
            } catch (Exception e) {
                log.error("Lỗi khi gửi nhắc nhở cho user {}: {}", 
                        reminder.getUser().getEmail(), e.getMessage(), e);
            }
        }
        
        log.info("Hoàn thành gửi nhắc nhở. Thành công: {}, Bỏ qua: {}", successCount, skipCount);
    }
}

