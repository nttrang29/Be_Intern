package com.example.financeapp.notification.service;

import com.example.financeapp.notification.dto.NotificationResponse;
import com.example.financeapp.notification.entity.Notification;

import java.util.List;

/**
 * Service để quản lý thông báo
 */
public interface NotificationService {

    /**
     * Tạo thông báo cho user cụ thể
     */
    Notification createUserNotification(
            Long userId,
            Notification.NotificationType type,
            String title,
            String message,
            Long referenceId,
            String referenceType
    );

    /**
     * Tạo thông báo cho tất cả admin
     */
    Notification createAdminNotification(
            Notification.NotificationType type,
            String title,
            String message,
            Long referenceId,
            String referenceType
    );

    /**
     * Lấy tất cả thông báo của user
     */
    List<NotificationResponse> getUserNotifications(Long userId);

    /**
     * Lấy tất cả thông báo của admin
     */
    List<NotificationResponse> getAdminNotifications();

    /**
     * Lấy thông báo chưa đọc của user
     */
    List<NotificationResponse> getUnreadUserNotifications(Long userId);

    /**
     * Lấy thông báo chưa đọc của admin
     */
    List<NotificationResponse> getUnreadAdminNotifications();

    /**
     * Đánh dấu thông báo đã đọc
     */
    NotificationResponse markAsRead(Long notificationId, Long userId, String role);

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    void markAllAsRead(Long userId, String role);

    /**
     * Đếm số thông báo chưa đọc
     */
    long countUnreadNotifications(Long userId, String role);

    /**
     * Xóa thông báo
     */
    void deleteNotification(Long notificationId, Long userId, String role);
}

