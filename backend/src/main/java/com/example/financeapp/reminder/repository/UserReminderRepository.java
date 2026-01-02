package com.example.financeapp.reminder.repository;

import com.example.financeapp.reminder.entity.UserReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface UserReminderRepository extends JpaRepository<UserReminder, Long> {

    /**
     * Tìm reminder của user
     */
    Optional<UserReminder> findByUser_UserId(Long userId);

    /**
     * Tìm tất cả user cần nhắc nhở tại thời điểm hiện tại
     * - enabled = true
     * - reminderTime <= currentTime
     * - lastReminderDate != today (chưa gửi nhắc nhở hôm nay)
     */
    @Query("""
        SELECT ur FROM UserReminder ur
        WHERE ur.enabled = true
          AND ur.reminderTime <= :currentTime
          AND (ur.lastReminderDate IS NULL OR ur.lastReminderDate < :today)
        """)
    List<UserReminder> findUsersToRemind(
            @Param("currentTime") LocalTime currentTime,
            @Param("today") LocalDate today
    );

    /**
     * Kiểm tra user có reminder không
     */
    boolean existsByUser_UserId(Long userId);
}

