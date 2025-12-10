package com.example.financeapp.fund.scheduler;

import com.example.financeapp.email.EmailService;
import com.example.financeapp.notification.service.NotificationService;
import com.example.financeapp.wallet.repository.WalletTransferRepository;
import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundTransaction;
import com.example.financeapp.fund.entity.FundTransactionStatus;
import com.example.financeapp.fund.entity.FundTransactionType;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.fund.repository.FundTransactionRepository;
import com.example.financeapp.fund.service.FundService;
import com.example.financeapp.wallet.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WalletTransferRepository walletTransferRepository;

    @Autowired
    private FundTransactionRepository fundTransactionRepository;

    /**
     * Chạy mỗi phút để kiểm tra và thực hiện tự động nạp tiền
     * Cron: 0 * * * * * (mỗi phút)
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeAutoDeposits() {
        log.debug("Bắt đầu kiểm tra tự động nạp quỹ...");

        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalTime currentTime = currentDateTime.toLocalTime();
        LocalDate today = currentDateTime.toLocalDate();

        // Khoảng thời gian kiểm tra: từ 1 phút trước đến bây giờ
        LocalTime startTime = currentTime.minusMinutes(1);

        List<Fund> fundsToDeposit = new ArrayList<>();

        // 1. Tìm quỹ tự động nạp DAILY
        List<Fund> dailyDeposits = fundRepository.findDailyAutoDeposits(startTime, currentTime, currentDateTime);
        fundsToDeposit.addAll(dailyDeposits);
        log.debug("Tìm thấy {} quỹ cần tự động nạp DAILY", dailyDeposits.size());

        // 2. Tìm quỹ tự động nạp WEEKLY
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        // Tính toán start và end của tuần (thứ 2 đầu tuần đến chủ nhật cuối tuần)
        java.time.DayOfWeek firstDayOfWeek = today.getDayOfWeek();
        int daysFromMonday = (firstDayOfWeek.getValue() + 6) % 7; // 0=Monday, 6=Sunday
        LocalDateTime startOfWeek = currentDateTime.minusDays(daysFromMonday).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);
        List<Fund> weeklyDeposits = fundRepository.findWeeklyAutoDeposits(dayOfWeek, startTime, currentTime, currentDateTime, startOfWeek, endOfWeek);
        fundsToDeposit.addAll(weeklyDeposits);
        log.debug("Tìm thấy {} quỹ cần tự động nạp WEEKLY (thứ {})", weeklyDeposits.size(), dayOfWeek);

        // 3. Tìm quỹ tự động nạp MONTHLY
        int dayOfMonth = today.getDayOfMonth();
        List<Fund> monthlyDeposits = fundRepository.findMonthlyAutoDeposits(dayOfMonth, startTime, currentTime, currentDateTime);
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

                try {
                    var sourceWallet = fund.getSourceWallet();
                    if (sourceWallet != null && fund.getAutoDepositAmount() != null) {
                        java.math.BigDecimal shortage = fund.getAutoDepositAmount().subtract(
                                sourceWallet.getBalance() != null ? sourceWallet.getBalance() : java.math.BigDecimal.ZERO);
                        if (shortage.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            fund.setPendingAutoTopupAmount(shortage);
                            fund.setPendingAutoTopupAt(LocalDateTime.now());
                            fundRepository.save(fund);
                        }
                    }
                } catch (Exception ignore) {
                    // ignore pending set errors
                }

                // Lưu lịch sử giao dịch thất bại
                try {
                    FundTransaction tx = new FundTransaction();
                    tx.setFund(fund);
                    tx.setAmount(fund.getAutoDepositAmount() != null ? fund.getAutoDepositAmount() : BigDecimal.ZERO);
                    tx.setType(FundTransactionType.AUTO_DEPOSIT);
                    tx.setStatus(FundTransactionStatus.FAILED);
                    tx.setMessage(e.getMessage());
                    tx.setPerformedBy(fund.getOwner());
                    fundTransactionRepository.save(tx);
                } catch (Exception txErr) {
                    log.error("Không thể ghi lịch sử auto-deposit thất bại: {}", txErr.getMessage());
                }

                // Gửi email cảnh báo thất bại
                try {
                    emailService.sendAutoDepositFailedEmail(
                            fund.getOwner().getEmail(),
                            fund.getOwner().getFullName(),
                            fund.getFundName(),
                            e.getMessage()
                    );
                } catch (Exception emailErr) {
                    log.error("Lỗi khi gửi email auto-deposit failed: {}", emailErr.getMessage());
                }

                // Ghi một WalletTransfer với trạng thái CANCELLED để hiển thị lịch sử thất bại
                try {
                    var sourceWallet = fund.getSourceWallet();
                    var targetWallet = fund.getTargetWallet();
                    com.example.financeapp.wallet.entity.WalletTransfer cancelled = new com.example.financeapp.wallet.entity.WalletTransfer();
                    cancelled.setFromWallet(sourceWallet);
                    cancelled.setToWallet(targetWallet);
                    cancelled.setAmount(fund.getAutoDepositAmount() != null ? fund.getAutoDepositAmount() : java.math.BigDecimal.ZERO);
                    cancelled.setCurrencyCode(sourceWallet != null ? sourceWallet.getCurrencyCode() : null);
                    cancelled.setUser(fund.getOwner());
                    cancelled.setNote("Cố gắng nạp tự động thất bại: " + e.getMessage());
                    cancelled.setTransferDate(java.time.LocalDateTime.now());
                    cancelled.setStatus(com.example.financeapp.wallet.entity.WalletTransfer.TransferStatus.CANCELLED);
                    if (sourceWallet != null) {
                        cancelled.setFromBalanceBefore(sourceWallet.getBalance());
                        cancelled.setFromBalanceAfter(sourceWallet.getBalance());
                    }
                    if (targetWallet != null) {
                        cancelled.setToBalanceBefore(targetWallet.getBalance());
                        cancelled.setToBalanceAfter(targetWallet.getBalance());
                    }
                    walletTransferRepository.save(cancelled);
                } catch (Exception txErr) {
                    log.error("Lỗi khi ghi WalletTransfer cancelled: {}", txErr.getMessage());
                }

                // Tạo thông báo in-app
                try {
                    notificationService.createFundAutoDepositFailedNotification(
                            fund.getOwner().getUserId(),
                            fund.getFundId(),
                            fund.getFundName(),
                            e.getMessage()
                    );
                } catch (Exception notifError) {
                    log.error("Lỗi khi tạo notification auto-deposit failed: {}", notifError.getMessage());
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
        var result = fundService.depositToFund(
                fund.getOwner().getUserId(),
                fund.getFundId(),
                amount,
                com.example.financeapp.fund.entity.FundTransactionType.AUTO_DEPOSIT,
                "Tự động nạp tiền theo lịch"
        );

        // Lấy số dư mới trong quỹ sau khi nạp
        BigDecimal newBalance = result.getCurrentAmount();

        log.info("Đã tự động nạp {} {} vào quỹ '{}' (ID: {}) từ ví '{}'",
                amount,
                fund.getTargetWallet().getCurrencyCode(),
                fund.getFundName(),
                fund.getFundId(),
                sourceWallet.getWalletName());
        // Gửi email thông báo (nếu cấu hình) và tạo notification trong hệ thống
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

        try {
            String title = "Nạp tự động thành công: " + fund.getFundName();
            String message = "Đã tự động nạp " + String.format("%,.0f", amount) + " " + fund.getTargetWallet().getCurrencyCode() +
                    " vào quỹ '" + fund.getFundName() + "'. Số dư hiện tại: " + String.format("%,.0f", newBalance);
            notificationService.createUserNotification(
                    fund.getOwner().getUserId(),
                    com.example.financeapp.notification.entity.Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    title,
                    message,
                    fund.getFundId(),
                    "FUND_AUTO_DEPOSIT_SUCCESS"
            );
        } catch (Exception notifErr) {
            log.error("Lỗi khi tạo notification auto-deposit success: {}", notifErr.getMessage());
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

            try {
                String title = "Quỹ đạt mục tiêu: " + fund.getFundName();
                String message = "Quỹ '" + fund.getFundName() + "' đã đạt mục tiêu " + String.format("%,.0f", fund.getTargetAmount()) + " " + fund.getTargetWallet().getCurrencyCode();
                notificationService.createUserNotification(
                        fund.getOwner().getUserId(),
                        com.example.financeapp.notification.entity.Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        title,
                        message,
                        fund.getFundId(),
                        "FUND_COMPLETED"
                );
            } catch (Exception notifErr) {
                log.error("Lỗi khi tạo notification fund completed: {}", notifErr.getMessage());
            }
        }
    }
}

