package com.example.financeapp.review.dto;

import com.example.financeapp.review.entity.AppReview;
import java.time.LocalDateTime;

/**
 * DTO response cho đánh giá ứng dụng
 */
public class AppReviewResponse {

    private Long reviewId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String displayName;
    private Integer rating;
    private String content;
    private String status;
    private String adminReply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Factory method từ entity
    public static AppReviewResponse fromEntity(AppReview review) {
        AppReviewResponse response = new AppReviewResponse();
        response.setReviewId(review.getReviewId());
        response.setUserId(review.getUser().getUserId());
        response.setUserEmail(review.getUser().getEmail());
        response.setUserName(review.getUser().getFullName());
        response.setDisplayName(review.getDisplayName() != null && !review.getDisplayName().trim().isEmpty()
                ? review.getDisplayName()
                : "Người dùng ẩn danh");
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setStatus(review.getStatus().name());
        response.setAdminReply(review.getAdminReply());
        response.setRepliedAt(review.getRepliedAt());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }

    // Getters & Setters
    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

