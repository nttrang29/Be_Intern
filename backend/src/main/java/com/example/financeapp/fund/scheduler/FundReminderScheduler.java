package com.example.financeapp.fund.scheduler;

import com.example.financeapp.email.EmailService;
import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.notification.service.NotificationService;
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

    @Autowired
    private NotificationService notificationService;

    /**
     * Chạy mỗi phút để kiểm tra và gửi nhắc nhở
     * Cron: 0 * * * * * (mỗi phút) - để gửi đúng thời gian đã thiết lập
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendFundReminders() {
        log.debug("Bắt đầu kiểm tra nhắc nhở quỹ...");

        LocalTime currentTime = LocalTime.now();
        LocalDate today = LocalDate.now();

        // Khoảng thời gian kiểm tra: từ 1 phút trước đến bây giờ
        // Để chỉ tìm quỹ có reminderTime trong phút vừa qua
        LocalTime startTime = currentTime.minusMinutes(1);

        List<Fund> fundsToRemind = new ArrayList<>();

        // 1. Tìm quỹ nhắc nhở DAILY
        List<Fund> dailyReminders = fundRepository.findDailyReminders(startTime, currentTime, today);
        fundsToRemind.addAll(dailyReminders);
        log.debug("Tìm thấy {} quỹ cần nhắc nhở DAILY", dailyReminders.size());

        // 2. Tìm quỹ nhắc nhở WEEKLY
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        // Tính toán start và end của tuần (thứ 2 đầu tuần đến chủ nhật cuối tuần)
        java.time.DayOfWeek firstDayOfWeek = today.getDayOfWeek();
        int daysFromMonday = (firstDayOfWeek.getValue() + 6) % 7; // 0=Monday, 6=Sunday
        java.time.LocalDateTime startOfWeek = today.atStartOfDay().minusDays(daysFromMonday).withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        List<Fund> weeklyReminders = fundRepository.findWeeklyReminders(dayOfWeek, startTime, currentTime, startOfWeek, endOfWeek);
        fundsToRemind.addAll(weeklyReminders);
        log.debug("Tìm thấy {} quỹ cần nhắc nhở WEEKLY (thứ {})", weeklyReminders.size(), dayOfWeek);

        // 3. Tìm quỹ nhắc nhở MONTHLY
        int dayOfMonth = today.getDayOfMonth();
        List<Fund> monthlyReminders = fundRepository.findMonthlyReminders(dayOfMonth, startTime, currentTime, today);
        fundsToRemind.addAll(monthlyReminders);
        log.debug("Tìm thấy {} quỹ cần nhắc nhở MONTHLY (ngày {})", monthlyReminders.size(), dayOfMonth);

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
     * Gửi email và notification nhắc nhở nạp quỹ
     */
    private void sendReminderNotification(Fund fund) {
        String currentAmountStr = String.format("%,.0f", fund.getCurrentAmount());
        String targetAmountStr = fund.getTargetAmount() != null
                ? String.format("%,.0f", fund.getTargetAmount())
                : null;
        String currency = fund.getTargetWallet().getCurrencyCode();

        // Gửi email nhắc nhở
        try {
            emailService.sendFundReminderEmail(
                    fund.getOwner().getEmail(),
                    fund.getOwner().getFullName(),
                    fund.getFundName(),
                    currentAmountStr,
                    targetAmountStr,
                    currency
            );

            log.info("Đã gửi email nhắc nhở cho quỹ '{}' (ID: {}) đến {}",
                    fund.getFundName(), fund.getFundId(), fund.getOwner().getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email nhắc nhở quỹ ID {}: {}", fund.getFundId(), e.getMessage());
            // Không throw exception để không làm fail toàn bộ scheduler
        }

        // Tạo notification trong hệ thống
        try {
            String title = "Nhắc nhở nạp quỹ: " + fund.getFundName();
            String message = "Đã đến lúc nạp tiền vào quỹ '" + fund.getFundName() + "'. " +
                    "Số dư hiện tại: " + currentAmountStr + " " + currency +
                    (targetAmountStr != null ? ". Mục tiêu: " + targetAmountStr + " " + currency : "");

            notificationService.createUserNotification(
                    fund.getOwner().getUserId(),
                    com.example.financeapp.notification.entity.Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    title,
                    message,
                    fund.getFundId(),
                    "FUND_REMINDER"
            );

            log.info("Đã tạo notification nhắc nhở cho quỹ '{}' (ID: {})",
                    fund.getFundName(), fund.getFundId());
        } catch (Exception e) {
            log.error("Lỗi khi tạo notification nhắc nhở quỹ ID {}: {}",
                    fund.getFundId(), e.getMessage());
            // Không throw exception để không làm fail toàn bộ scheduler
        }
    }
}

