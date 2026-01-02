package com.example.financeapp.review.entity;

import com.example.financeapp.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity cho đánh giá ứng dụng từ người dùng
 */
@Entity
@Table(name = "app_reviews")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AppReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "provider", 
            "enabled", "locked", "deleted", "resetToken", "resetTokenExpiredAt", "lastActiveAt"})
    private User user;

    @Column(name = "display_name", length = 100)
    private String displayName; // Tên hiển thị (mặc định "Người dùng ẩn danh" nếu null)

    @Column(name = "rating", nullable = false)
    private Integer rating; // 1-5 sao

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content; // Nội dung đánh giá

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING; // PENDING, ANSWERED

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply; // Phản hồi từ admin

    @Column(name = "replied_at")
    private LocalDateTime repliedAt; // Thời gian admin phản hồi

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Enum cho trạng thái đánh giá
    public enum ReviewStatus {
        PENDING,  // Chờ admin trả lời
        ANSWERED  // Admin đã phản hồi
    }

    // Getters & Setters
    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

