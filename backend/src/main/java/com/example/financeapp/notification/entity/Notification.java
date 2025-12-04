package com.example.financeapp.notification.entity;

import com.example.financeapp.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity cho hệ thống thông báo
 */
@Entity
@Table(name = "notifications")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "provider", 
            "enabled", "locked", "deleted", "resetToken", "resetTokenExpiredAt", "lastActiveAt"})
    private User user; // null nếu gửi tới tất cả admin

    @Column(name = "receiver_role", length = 20)
    private String receiverRole; // "ADMIN", "USER", null

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "reference_id")
    private Long referenceId; // ID của đối tượng liên quan (reviewId, feedbackId, etc.)

    @Column(name = "reference_type", length = 50)
    private String referenceType; // "APP_REVIEW", "FEEDBACK", etc.

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Enum cho loại thông báo
    public enum NotificationType {
        NEW_APP_REVIEW,         // Admin nhận: có đánh giá mới
        REVIEW_REPLIED,         // User nhận: admin đã phản hồi
        NEW_FEEDBACK,           // Admin nhận: có feedback mới
        FEEDBACK_REPLIED,       // User nhận: admin đã phản hồi feedback
        BUDGET_WARNING,         // User nhận: ngân sách sắp hết
        BUDGET_EXCEEDED,        // User nhận: ngân sách vượt hạn mức
        FUND_AUTO_DEPOSIT_SUCCESS,  // User nhận: tự động nạp quỹ thành công
        FUND_AUTO_DEPOSIT_FAILED,   // User nhận: tự động nạp quỹ thất bại
        FUND_COMPLETED,         // User nhận: quỹ đã đạt mục tiêu
        SYSTEM_ANNOUNCEMENT     // Thông báo hệ thống
    }

    // Getters & Setters
    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReceiverRole() {
        return receiverRole;
    }

    public void setReceiverRole(String receiverRole) {
        this.receiverRole = receiverRole;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

