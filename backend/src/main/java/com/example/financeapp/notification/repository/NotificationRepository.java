package com.example.financeapp.notification.repository;

import com.example.financeapp.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả thông báo của user cụ thể, sắp xếp mới nhất trước
     */
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lấy thông báo theo role (dành cho admin)
     */
    List<Notification> findByReceiverRoleOrderByCreatedAtDesc(String role);

    /**
     * Lấy thông báo chưa đọc của user
     */
    List<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Lấy thông báo chưa đọc theo role
     */
    List<Notification> findByReceiverRoleAndIsReadFalseOrderByCreatedAtDesc(String role);

    /**
     * Đếm số thông báo chưa đọc của user
     */
    long countByUser_UserIdAndIsReadFalse(Long userId);

    /**
     * Đếm số thông báo chưa đọc theo role
     */
    long countByReceiverRoleAndIsReadFalse(String role);

    /**
     * Lấy tất cả thông báo cho admin (receiverRole = "ADMIN")
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.receiverRole = 'ADMIN'
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findAllAdminNotifications();

    /**
     * Lấy tất cả thông báo cho một user (bao gồm cả thông báo hệ thống)
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.userId = :userId OR (n.user IS NULL AND n.receiverRole = 'USER')
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findAllUserNotifications(@Param("userId") Long userId);
}

