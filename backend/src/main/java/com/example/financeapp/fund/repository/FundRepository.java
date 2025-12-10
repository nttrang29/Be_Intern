package com.example.financeapp.fund.repository;

import com.example.financeapp.fund.entity.Fund;
import com.example.financeapp.fund.entity.FundStatus;
import com.example.financeapp.fund.entity.FundType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FundRepository extends JpaRepository<Fund, Long> {

    /**
     * Lấy tất cả quỹ của user (cả quỹ cá nhân và quỹ nhóm mà user tham gia)
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        LEFT JOIN FundMember fm ON fm.fund.fundId = f.fundId
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND (f.owner.userId = :userId OR fm.user.userId = :userId)
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByUserInvolved(@Param("userId") Long userId);

    /**
     * Lấy quỹ cá nhân của user
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.owner.userId = :userId AND f.fundType = :fundType
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByOwner_UserIdAndFundTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("fundType") FundType fundType
    );

    /**
     * Lấy quỹ nhóm mà user là chủ quỹ
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.owner.userId = :userId 
          AND f.fundType = :fundType 
          AND f.status = :status
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByOwner_UserIdAndFundTypeAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("fundType") FundType fundType,
            @Param("status") FundStatus status
    );

    /**
     * Kiểm tra ví đã được sử dụng cho quỹ hoặc ngân sách chưa
     */
    @Query("""
        SELECT COUNT(f) > 0 FROM Fund f
        WHERE f.targetWallet.walletId = :walletId
        """)
    boolean existsByTargetWallet_WalletId(@Param("walletId") Long walletId);

    /**
     * Lấy quỹ theo status
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.status = :status
        ORDER BY f.createdAt DESC
        """)
    List<Fund> findByStatusOrderByCreatedAtDesc(@Param("status") FundStatus status);

    /**
     * Lấy quỹ có deadline và đang active
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.hasDeadline = true
          AND f.status = 'ACTIVE'
          AND f.endDate <= :today
        """)
    List<Fund> findExpiredActiveFunds(@Param("today") java.time.LocalDate today);

    /**
     * Lấy quỹ theo ID với eager load các entity liên quan
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE f.fundId = :fundId
        """)
    java.util.Optional<Fund> findByIdWithRelations(@Param("fundId") Long fundId);

    /**
     * Lấy tất cả quỹ có targetWalletId = walletId
     */
    @Query("""
        SELECT f FROM Fund f
        WHERE f.targetWallet.walletId = :walletId
        """)
    List<Fund> findByTargetWallet_WalletId(@Param("walletId") Long walletId);

    /**
     * Tìm các quỹ có pending auto topup từ source wallet
     */
    @Query("""
        SELECT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.sourceWallet.walletId = :walletId
          AND f.pendingAutoTopupAmount IS NOT NULL
          AND f.pendingAutoTopupAmount > 0
        """)
    List<Fund> findPendingAutoTopupBySourceWallet(@Param("walletId") Long walletId);

    /**
     * Tìm các quỹ cần nhắc nhở theo DAILY
     * Chỉ lấy quỹ chưa được nhắc nhở trong ngày hôm nay
     * Tìm quỹ có reminderTime trong khoảng từ startTime đến currentTime (phút vừa qua)
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.reminderEnabled = true
          AND f.reminderType = 'DAILY'
          AND f.status = 'ACTIVE'
          AND f.reminderTime <= :currentTime
          AND f.reminderTime >= :startTime
          AND NOT EXISTS (
              SELECT 1 FROM Notification n
              WHERE n.user.userId = f.owner.userId
                  AND n.referenceId = f.fundId
                  AND n.type = 'SYSTEM_ANNOUNCEMENT'
                  AND n.referenceType = 'FUND_REMINDER'
                  AND DATE(n.createdAt) = DATE(:currentDate)
          )
        """)
    List<Fund> findDailyReminders(
            @Param("startTime") java.time.LocalTime startTime,
            @Param("currentTime") java.time.LocalTime currentTime,
            @Param("currentDate") java.time.LocalDate currentDate
    );

    /**
     * Tìm các quỹ cần nhắc nhở theo WEEKLY
     * Chỉ lấy quỹ chưa được nhắc nhở trong tuần này
     * Tìm quỹ có reminderTime trong khoảng từ startTime đến currentTime (phút vừa qua)
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.reminderEnabled = true
          AND f.reminderType = 'WEEKLY'
          AND f.status = 'ACTIVE'
          AND f.reminderDayOfWeek = :dayOfWeek
          AND f.reminderTime <= :currentTime
          AND f.reminderTime >= :startTime
          AND NOT EXISTS (
              SELECT 1 FROM Notification n
              WHERE n.user.userId = f.owner.userId
                  AND n.referenceId = f.fundId
                  AND n.type = 'SYSTEM_ANNOUNCEMENT'
                  AND n.referenceType = 'FUND_REMINDER'
                  AND n.createdAt >= :startOfWeek
                  AND n.createdAt <= :endOfWeek
          )
        """)
    List<Fund> findWeeklyReminders(
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("currentTime") java.time.LocalTime currentTime,
            @Param("startOfWeek") java.time.LocalDateTime startOfWeek,
            @Param("endOfWeek") java.time.LocalDateTime endOfWeek
    );

    /**
     * Tìm các quỹ cần nhắc nhở theo MONTHLY
     * Chỉ lấy quỹ chưa được nhắc nhở trong tháng này
     * Tìm quỹ có reminderTime trong khoảng từ startTime đến currentTime (phút vừa qua)
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
        WHERE (f.deleted = false OR f.deleted IS NULL)
          AND f.reminderEnabled = true
          AND f.reminderType = 'MONTHLY'
          AND f.status = 'ACTIVE'
          AND f.reminderDayOfMonth = :dayOfMonth
          AND f.reminderTime <= :currentTime
          AND f.reminderTime >= :startTime
          AND NOT EXISTS (
              SELECT 1 FROM Notification n
              WHERE n.user.userId = f.owner.userId
                  AND n.referenceId = f.fundId
                  AND n.type = 'SYSTEM_ANNOUNCEMENT'
                  AND n.referenceType = 'FUND_REMINDER'
                  AND YEAR(n.createdAt) = YEAR(:currentDate)
                  AND MONTH(n.createdAt) = MONTH(:currentDate)
          )
        """)
    List<Fund> findMonthlyReminders(
            @Param("dayOfMonth") Integer dayOfMonth,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("currentTime") java.time.LocalTime currentTime,
            @Param("currentDate") java.time.LocalDate currentDate
    );

    /**
     * Tìm các quỹ cần tự động nạp tiền theo DAILY
     * Chỉ lấy quỹ chưa được nạp trong ngày hôm nay
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
                WHERE (f.deleted = false OR f.deleted IS NULL)
                    AND f.autoDepositEnabled = true
                    AND f.autoDepositScheduleType = 'DAILY'
                    AND f.status = 'ACTIVE'
                    AND f.autoDepositTime <= :currentTime
                    AND f.autoDepositTime >= :startTime
                    AND (f.autoDepositStartAt IS NULL OR f.autoDepositStartAt <= :currentDateTime)
                    AND NOT EXISTS (
                        SELECT 1 FROM FundTransaction tx
                        WHERE tx.fund.fundId = f.fundId
                            AND tx.type = 'AUTO_DEPOSIT'
                            AND tx.status = 'SUCCESS'
                            AND DATE(tx.createdAt) = DATE(:currentDateTime)
                    )
        """)
    List<Fund> findDailyAutoDeposits(
            @Param("startTime") java.time.LocalTime startTime,
            @Param("currentTime") java.time.LocalTime currentTime,
            @Param("currentDateTime") java.time.LocalDateTime currentDateTime
    );

    /**
     * Tìm các quỹ cần tự động nạp tiền theo WEEKLY
     * Chỉ lấy quỹ chưa được nạp trong tuần này (từ startOfWeek đến endOfWeek)
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
                WHERE (f.deleted = false OR f.deleted IS NULL)
                    AND f.autoDepositEnabled = true
                    AND f.autoDepositScheduleType = 'WEEKLY'
                    AND f.status = 'ACTIVE'
                    AND f.autoDepositDayOfWeek = :dayOfWeek
                    AND f.autoDepositTime <= :currentTime
                    AND f.autoDepositTime >= :startTime
                    AND (f.autoDepositStartAt IS NULL OR f.autoDepositStartAt <= :currentDateTime)
                    AND NOT EXISTS (
                        SELECT 1 FROM FundTransaction tx
                        WHERE tx.fund.fundId = f.fundId
                            AND tx.type = 'AUTO_DEPOSIT'
                            AND tx.status = 'SUCCESS'
                            AND tx.createdAt >= :startOfWeek
                            AND tx.createdAt <= :endOfWeek
                    )
        """)
    List<Fund> findWeeklyAutoDeposits(
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("currentTime") java.time.LocalTime currentTime,
            @Param("currentDateTime") java.time.LocalDateTime currentDateTime,
            @Param("startOfWeek") java.time.LocalDateTime startOfWeek,
            @Param("endOfWeek") java.time.LocalDateTime endOfWeek
    );

    /**
     * Tìm các quỹ cần tự động nạp tiền theo MONTHLY
     * Chỉ lấy quỹ chưa được nạp trong tháng này
     */
    @Query("""
        SELECT DISTINCT f FROM Fund f
        LEFT JOIN FETCH f.owner
        LEFT JOIN FETCH f.targetWallet
        LEFT JOIN FETCH f.sourceWallet
                WHERE (f.deleted = false OR f.deleted IS NULL)
                    AND f.autoDepositEnabled = true
                    AND f.autoDepositScheduleType = 'MONTHLY'
                    AND f.status = 'ACTIVE'
                    AND f.autoDepositDayOfMonth = :dayOfMonth
                    AND f.autoDepositTime <= :currentTime
                    AND f.autoDepositTime >= :startTime
                    AND (f.autoDepositStartAt IS NULL OR f.autoDepositStartAt <= :currentDateTime)
                    AND NOT EXISTS (
                        SELECT 1 FROM FundTransaction tx
                        WHERE tx.fund.fundId = f.fundId
                            AND tx.type = 'AUTO_DEPOSIT'
                            AND tx.status = 'SUCCESS'
                            AND YEAR(tx.createdAt) = YEAR(:currentDateTime)
                            AND MONTH(tx.createdAt) = MONTH(:currentDateTime)
                    )
        """)
    List<Fund> findMonthlyAutoDeposits(
            @Param("dayOfMonth") Integer dayOfMonth,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("currentTime") java.time.LocalTime currentTime,
            @Param("currentDateTime") java.time.LocalDateTime currentDateTime
    );
}

