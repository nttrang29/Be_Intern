package com.example.financeapp.notification.service.impl;

import com.example.financeapp.notification.dto.NotificationResponse;
import com.example.financeapp.notification.entity.Notification;
import com.example.financeapp.notification.repository.NotificationRepository;
import com.example.financeapp.notification.service.NotificationService;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Notification createUserNotification(
            Long userId,
            Notification.NotificationType type,
            String title,
            String message,
            Long referenceId,
            String referenceType
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setReceiverRole(null);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("Đã tạo thông báo cho user {}: {}", user.getEmail(), title);

        return saved;
    }

    @Override
    @Transactional
    public Notification createAdminNotification(
            Notification.NotificationType type,
            String title,
            String message,
            Long referenceId,
            String referenceType
    ) {
        Notification notification = new Notification();
        notification.setUser(null);
        notification.setReceiverRole("ADMIN");
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("Đã tạo thông báo cho admin: {}", title);

        return saved;
    }

    @Override
    public List<NotificationResponse> getUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findAllUserNotifications(userId);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getAdminNotifications() {
        List<Notification> notifications = notificationRepository.findAllAdminNotifications();
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadAdminNotifications() {
        List<Notification> notifications = notificationRepository
                .findByReceiverRoleAndIsReadFalseOrderByCreatedAtDesc("ADMIN");
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId, String role) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        // Kiểm tra quyền
        if ("ADMIN".equals(role)) {
            if (!"ADMIN".equals(notification.getReceiverRole())) {
                throw new RuntimeException("Bạn không có quyền đánh dấu thông báo này");
            }
        } else {
            if (notification.getUser() == null || !notification.getUser().getUserId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền đánh dấu thông báo này");
            }
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId, String role) {
        List<Notification> notifications;

        if ("ADMIN".equals(role)) {
            notifications = notificationRepository
                    .findByReceiverRoleAndIsReadFalseOrderByCreatedAtDesc("ADMIN");
        } else {
            notifications = notificationRepository
                    .findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        }

        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        notificationRepository.saveAll(notifications);
        log.info("Đã đánh dấu {} thông báo là đã đọc", notifications.size());
    }

    @Override
    public long countUnreadNotifications(Long userId, String role) {
        if ("ADMIN".equals(role)) {
            return notificationRepository.countByReceiverRoleAndIsReadFalse("ADMIN");
        } else {
            return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
        }
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId, String role) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        // Kiểm tra quyền
        if ("ADMIN".equals(role)) {
            if (!"ADMIN".equals(notification.getReceiverRole())) {
                throw new RuntimeException("Bạn không có quyền xóa thông báo này");
            }
        } else {
            if (notification.getUser() == null || !notification.getUser().getUserId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền xóa thông báo này");
            }
        }

        notificationRepository.delete(notification);
        log.info("Đã xóa thông báo ID: {}", notificationId);
    }
}

