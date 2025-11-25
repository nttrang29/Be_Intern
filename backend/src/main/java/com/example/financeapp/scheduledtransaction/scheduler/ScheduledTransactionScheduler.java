package com.example.financeapp.scheduledtransaction.scheduler;

import com.example.financeapp.scheduledtransaction.entity.ScheduledTransaction;
import com.example.financeapp.scheduledtransaction.repository.ScheduledTransactionRepository;
import com.example.financeapp.scheduledtransaction.service.ScheduledTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduler để tự động thực hiện các scheduled transactions
 * Chạy mỗi phút để kiểm tra và thực hiện các giao dịch đến hạn
 */
@Component
public class ScheduledTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTransactionScheduler.class);

    @Autowired
    private ScheduledTransactionRepository scheduledTransactionRepository;
    
    @Autowired
    private ScheduledTransactionService scheduledTransactionService;

    /**
     * Chạy mỗi phút để kiểm tra và thực hiện scheduled transactions
     * Cron: 0 * * * * * (mỗi phút)
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeScheduledTransactions() {
        log.debug("Bắt đầu kiểm tra scheduled transactions...");
        
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        
        // Tìm các scheduled transactions cần thực hiện
        List<ScheduledTransaction> toExecute = scheduledTransactionRepository
                .findPendingTransactionsToExecute(today, currentTime);
        
        if (toExecute.isEmpty()) {
            log.debug("Không có scheduled transaction nào cần thực hiện");
            return;
        }
        
        log.info("Tìm thấy {} scheduled transactions cần thực hiện", toExecute.size());
        
        int successCount = 0;
        int failedCount = 0;
        
        for (ScheduledTransaction scheduled : toExecute) {
            try {
                scheduledTransactionService.executeScheduledTransaction(scheduled);
                successCount++;
                log.info("Đã thực hiện scheduled transaction ID: {} cho user: {}", 
                        scheduled.getScheduleId(), scheduled.getUser().getEmail());
            } catch (Exception e) {
                failedCount++;
                log.error("Lỗi khi thực hiện scheduled transaction ID {}: {}", 
                        scheduled.getScheduleId(), e.getMessage(), e);
            }
        }
        
        log.info("Hoàn thành thực hiện scheduled transactions. Thành công: {}, Thất bại: {}", 
                successCount, failedCount);
    }
}

