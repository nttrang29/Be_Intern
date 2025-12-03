package com.example.financeapp.fund.scheduler;

import com.example.financeapp.email.EmailService;
import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.fund.service.FundService;
import com.example.financeapp.wallet.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler để tự động nạp tiền vào quỹ
 * Chạy mỗi phút để kiểm tra quỹ nào cần tự động nạp tiền
 */
@Component
public class FundAutoDepositScheduler {

    private static final Logger log = LoggerFactory.getLogger(FundAutoDepositScheduler.class);

    @Autowired
    private FundRepository fundRepository;
    
    @Autowired
    private FundService fundService;
    
    @Autowired
    private EmailService emailService;

    /**
     * Chạy mỗi phút để kiểm tra và thực hiện tự động nạp tiền
     * Cron: 0 * * * * * (mỗi phút)
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeAutoDeposits() {
        log.debug("Bắt đầu kiểm tra tự động nạp quỹ...");
        
        LocalTime currentTime = LocalTime.now();
        LocalDate today = LocalDate.now();
        
        // Khoảng thời gian kiểm tra: từ 1 phút trước đến bây giờ
        LocalTime startTime = currentTime.minusMinutes(1);
        
        List<Fund> fundsToDeposit = new ArrayList<>();
        
        // 1. Tìm quỹ tự động nạp DAILY
        List<Fund> dailyDeposits = fundRepository.findDailyAutoDeposits(startTime, currentTime);
        fundsToDeposit.addAll(dailyDeposits);
        log.debug("Tìm thấy {} quỹ cần tự động nạp DAILY", dailyDeposits.size());
        
        // 2. Tìm quỹ tự động nạp WEEKLY
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        List<Fund> weeklyDeposits = fundRepository.findWeeklyAutoDeposits(dayOfWeek, startTime, currentTime);
        fundsToDeposit.addAll(weeklyDeposits);
        log.debug("Tìm thấy {} quỹ cần tự động nạp WEEKLY (thứ {})", weeklyDeposits.size(), dayOfWeek);
        
        // 3. Tìm quỹ tự động nạp MONTHLY
        int dayOfMonth = today.getDayOfMonth();
        List<Fund> monthlyDeposits = fundRepository.findMonthlyAutoDeposits(dayOfMonth, startTime, currentTime);
        fundsToDeposit.addAll(monthlyDeposits);
        log.debug("Tìm thấy {} quỹ cần tự động nạp MONTHLY (ngày {})", monthlyDeposits.size(), dayOfMonth);
        
        if (fundsToDeposit.isEmpty()) {
            log.debug("Không có quỹ nào cần tự động nạp tiền");
            return;
        }
        
        log.info("Tổng cộng {} quỹ cần tự động nạp tiền", fundsToDeposit.size());
        
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        
        for (Fund fund : fundsToDeposit) {
            try {
                executeAutoDeposit(fund);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("Lỗi khi tự động nạp tiền cho quỹ ID {}: {}", 
                        fund.getFundId(), e.getMessage(), e);
                
                // Gửi email thông báo lỗi
                try {
                    emailService.sendAutoDepositFailedEmail(
                        fund.getOwner().getEmail(),
                        fund.getOwner().getFullName(),
                        fund.getFundName(),
                        e.getMessage()
                    );
                } catch (Exception emailError) {
                    log.error("Lỗi khi gửi email auto-deposit failed: {}", emailError.getMessage());
                }
            }
        }
        
        log.info("=== Hoàn thành tự động nạp quỹ. Thành công: {}, Thất bại: {}, Bỏ qua: {} ===", 
                successCount, failedCount, skippedCount);
    }
    
    /**
     * Thực hiện tự động nạp tiền cho một quỹ
     */
    private void executeAutoDeposit(Fund fund) {
        BigDecimal amount = fund.getAutoDepositAmount();
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Quỹ ID {} có autoDepositAmount không hợp lệ: {}", fund.getFundId(), amount);
            throw new RuntimeException("Số tiền tự động nạp không hợp lệ");
        }
        
        Wallet sourceWallet = fund.getSourceWallet();
        if (sourceWallet == null) {
            log.warn("Quỹ ID {} không có ví nguồn", fund.getFundId());
            throw new RuntimeException("Không có ví nguồn để tự động nạp");
        }
        
        // Kiểm tra số dư ví nguồn
        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            log.warn("Ví nguồn ID {} không đủ số dư. Cần: {}, Có: {}", 
                    sourceWallet.getWalletId(), amount, sourceWallet.getBalance());
            throw new RuntimeException(String.format(
                "Số dư ví nguồn '%s' không đủ để tự động nạp %,.0f %s",
                sourceWallet.getWalletName(),
                amount,
                sourceWallet.getCurrencyCode()
            ));
        }
        
        // Thực hiện nạp tiền
        var result = fundService.depositToFund(fund.getOwner().getUserId(), fund.getFundId(), amount);
        
        // Lấy số dư mới sau khi nạp
        BigDecimal newBalance = result.getCurrentAmount();
        
        log.info("Đã tự động nạp {} {} vào quỹ '{}' (ID: {}) từ ví '{}'", 
                amount, 
                fund.getTargetWallet().getCurrencyCode(),
                fund.getFundName(), 
                fund.getFundId(),
                sourceWallet.getWalletName());
        
        // Gửi email thông báo
        try {
            emailService.sendAutoDepositSuccessEmail(
                fund.getOwner().getEmail(),
                fund.getOwner().getFullName(),
                fund.getFundName(),
                String.format("%,.0f", amount),
                String.format("%,.0f", newBalance),
                fund.getTargetWallet().getCurrencyCode(),
                sourceWallet.getWalletName()
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi email auto-deposit success: {}", e.getMessage());
        }
        
        // Kiểm tra nếu đạt mục tiêu thì gửi email chúc mừng
        if (fund.getTargetAmount() != null && newBalance.compareTo(fund.getTargetAmount()) >= 0) {
            try {
                emailService.sendFundCompletedEmail(
                    fund.getOwner().getEmail(),
                    fund.getOwner().getFullName(),
                    fund.getFundName(),
                    String.format("%,.0f", fund.getTargetAmount()),
                    fund.getTargetWallet().getCurrencyCode()
                );
                
                log.info("Quỹ '{}' (ID: {}) đã đạt mục tiêu!", fund.getFundName(), fund.getFundId());
            } catch (Exception e) {
                log.error("Lỗi khi gửi email fund completed: {}", e.getMessage());
            }
        }
    }
}

