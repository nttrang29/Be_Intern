package com.example.financeapp.fund.scheduler;

import com.example.financeapp.email.EmailService;
import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.repository.FundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler để gửi nhắc nhở nạp tiền vào quỹ
 * Chạy mỗi 30 phút để kiểm tra quỹ nào cần nhắc nhở
 */
@Component
public class FundReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(FundReminderScheduler.class);

    @Autowired
    private FundRepository fundRepository;
    
    @Autowired
    private EmailService emailService;

    /**
     * Chạy mỗi 30 phút để kiểm tra và gửi nhắc nhở
     * Cron: 0 0,30 * * * * (mỗi 30 phút - vào phút 0 và 30 của mỗi giờ)
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void sendFundReminders() {
        log.info("=== Bắt đầu kiểm tra nhắc nhở quỹ... ===");
        
        LocalTime currentTime = LocalTime.now();
        LocalDate today = LocalDate.now();
        
        // Khoảng thời gian kiểm tra: từ 30 phút trước đến bây giờ
        LocalTime startTime = currentTime.minusMinutes(30);
        
        List<Fund> fundsToRemind = new ArrayList<>();
        
        // 1. Tìm quỹ nhắc nhở DAILY
        List<Fund> dailyReminders = fundRepository.findDailyReminders(startTime, currentTime);
        fundsToRemind.addAll(dailyReminders);
        log.info("Tìm thấy {} quỹ cần nhắc nhở DAILY", dailyReminders.size());
        
        // 2. Tìm quỹ nhắc nhở WEEKLY
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        List<Fund> weeklyReminders = fundRepository.findWeeklyReminders(dayOfWeek, startTime, currentTime);
        fundsToRemind.addAll(weeklyReminders);
        log.info("Tìm thấy {} quỹ cần nhắc nhở WEEKLY (thứ {})", weeklyReminders.size(), dayOfWeek);
        
        // 3. Tìm quỹ nhắc nhở MONTHLY
        int dayOfMonth = today.getDayOfMonth();
        List<Fund> monthlyReminders = fundRepository.findMonthlyReminders(dayOfMonth, startTime, currentTime);
        fundsToRemind.addAll(monthlyReminders);
        log.info("Tìm thấy {} quỹ cần nhắc nhở MONTHLY (ngày {})", monthlyReminders.size(), dayOfMonth);
        
        if (fundsToRemind.isEmpty()) {
            log.info("Không có quỹ nào cần nhắc nhở");
            return;
        }
        
        log.info("Tổng cộng {} quỹ cần gửi nhắc nhở", fundsToRemind.size());
        
        int successCount = 0;
        int failedCount = 0;
        
        for (Fund fund : fundsToRemind) {
            try {
                sendReminderNotification(fund);
                successCount++;
                log.info("Đã gửi nhắc nhở cho quỹ '{}' (ID: {}) của user: {}", 
                        fund.getFundName(), fund.getFundId(), fund.getOwner().getEmail());
            } catch (Exception e) {
                failedCount++;
                log.error("Lỗi khi gửi nhắc nhở cho quỹ ID {}: {}", 
                        fund.getFundId(), e.getMessage(), e);
            }
        }
        
        log.info("=== Hoàn thành gửi nhắc nhở quỹ. Thành công: {}, Thất bại: {} ===", 
                successCount, failedCount);
    }
    
    /**
     * Gửi email nhắc nhở nạp quỹ
     */
    private void sendReminderNotification(Fund fund) {
        try {
            String currentAmountStr = String.format("%,.0f", fund.getCurrentAmount());
            String targetAmountStr = fund.getTargetAmount() != null 
                ? String.format("%,.0f", fund.getTargetAmount()) 
                : null;
            
            emailService.sendFundReminderEmail(
                fund.getOwner().getEmail(),
                fund.getOwner().getFullName(),
                fund.getFundName(),
                currentAmountStr,
                targetAmountStr,
                fund.getTargetWallet().getCurrencyCode()
            );
            
            log.info("Đã gửi email nhắc nhở cho quỹ '{}' (ID: {}) đến {}", 
                    fund.getFundName(), fund.getFundId(), fund.getOwner().getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email nhắc nhở quỹ ID {}: {}", fund.getFundId(), e.getMessage());
            // Không throw exception để không làm fail toàn bộ scheduler
        }
    }
}

