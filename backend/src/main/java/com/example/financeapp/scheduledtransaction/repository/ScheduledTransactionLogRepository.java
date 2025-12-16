package com.example.financeapp.scheduledtransaction.repository;

import com.example.financeapp.scheduledtransaction.entity.ScheduledTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledTransactionLogRepository extends JpaRepository<ScheduledTransactionLog, Long> {

    /**
     * Lấy lịch sử thực hiện của scheduled transaction, sắp xếp theo thời gian mới nhất
     */
    List<ScheduledTransactionLog> findByScheduledTransaction_ScheduleIdOrderByExecutionTimeDesc(Long scheduleId);

    /**
     * Đếm số lần thực hiện thành công
     */
    long countByScheduledTransaction_ScheduleIdAndStatus(Long scheduleId, ScheduledTransactionLog.LogStatus status);
}
