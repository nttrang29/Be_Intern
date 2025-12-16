package com.example.financeapp.scheduledtransaction.repository;

import com.example.financeapp.scheduledtransaction.entity.ScheduledTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduledTransactionRepository extends JpaRepository<ScheduledTransaction, Long> {

    /**
     * Lấy tất cả scheduled transactions của user
     */
    List<ScheduledTransaction> findByUser_UserIdOrderByNextExecutionDateAsc(Long userId);

    /**
     * Tìm các scheduled transactions cần thực hiện
     * - status = PENDING
     * - nextExecutionDate <= today
     * - executionTime <= currentTime
     * - (endDate IS NULL OR endDate >= today)
     *
     * Sử dụng JOIN FETCH để eager load các lazy entities (transactionType, wallet, category, user)
     * để tránh LazyInitializationException khi thực hiện trong scheduler
     */
    @Query("""
        SELECT DISTINCT st FROM ScheduledTransaction st
        LEFT JOIN FETCH st.transactionType
        LEFT JOIN FETCH st.wallet
        LEFT JOIN FETCH st.category
        LEFT JOIN FETCH st.user
        WHERE st.status = 'PENDING'
          AND st.nextExecutionDate <= :today
          AND st.executionTime <= :currentTime
          AND (st.endDate IS NULL OR st.endDate >= :today)
        ORDER BY st.nextExecutionDate ASC, st.executionTime ASC
        """)
    List<ScheduledTransaction> findPendingTransactionsToExecute(
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime
    );

    /**
     * Kiểm tra scheduled transaction có tồn tại không
     */
    boolean existsByScheduleIdAndUser_UserId(Long scheduleId, Long userId);
}

